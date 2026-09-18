package com.aiqingdian.service;

import com.aiqingdian.config.ArkProperties;
import com.aiqingdian.config.CvProperties;
import com.aiqingdian.dto.CvResult;
import com.aiqingdian.dto.CvTray;
import com.aiqingdian.exception.BadRequestException;
import com.aiqingdian.exception.CvException;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 路线 A：用 OpenCV（JavaCPP 绑定）在 JVM 内完成"餐盘检测 + 食物像素占比计算"，
 * 不需要 Python 服务，也不调用深度学习模型。
 *
 * 核心思路：
 * 1. 自适应阈值 + 形态学闭运算 + 轮廓检测，找到展示柜中的餐盘外接矩形；
 * 2. 对每个餐盘，用边框一圈像素估计空盘背景色（中位数，抗反光离群点）；
 * 3. 在餐盘内部把"与空盘背景色 Lab 距离足够大"的像素判为食物，算面积占比。
 */
@Service
public class FillRatioService {

    public static final String LEVEL_SUFFICIENT = "充足";
    public static final String LEVEL_MODERATE = "一般";
    public static final String LEVEL_LOW = "较少";
    public static final String LEVEL_CRITICAL = "严重不足";
    public static final String LEVEL_UNKNOWN = "未知";

    private static volatile boolean nativeLoaded = false;

    private final CvProperties cv;
    private final ArkProperties ark;

    public FillRatioService(CvProperties cv, ArkProperties ark) {
        this.cv = cv;
        this.ark = ark;
    }

    /**
     * 上传图片字节，返回检测到的餐盘、各自剩余占比，以及一张标注图（base64）。
     *
     * @param bytes   图片字节
     * @param gridRows 非空且大于 0 时启用等分网格模式（行数）
     * @param gridCols 非空且大于 0 时启用等分网格模式（列数）
     */
    public CvResult analyze(byte[] bytes, Integer gridRows, Integer gridCols) throws IOException {
        ensureNativeLoaded();

        BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
        if (original == null) {
            throw new BadRequestException("无法解析该图片，请上传 jpg/png/bmp/gif 格式的图片");
        }

        BufferedImage rgb = toIntRgb(original);
        int originalWidth = rgb.getWidth();
        int originalHeight = rgb.getHeight();

        BufferedImage processed = scaleToMaxSide(rgb, cv.getMaxSide());
        int processedWidth = processed.getWidth();
        int processedHeight = processed.getHeight();

        Mat bgr = toBgrMat(processed);
        Mat lab = null;
        Mat annotated = null;
        try {
            lab = new Mat();
            opencv_imgproc.cvtColor(bgr, lab, opencv_imgproc.COLOR_BGR2Lab);

            boolean grid = gridRows != null && gridRows > 0 && gridCols != null && gridCols > 0;
            Detection detection = grid
                    ? new Detection(detectGrid(bgr.cols(), bgr.rows(), gridRows, gridCols), 0, gridRows * gridCols)
                    : detectAuto(bgr);
            List<Rect> rects = detection.rects;

            double scaleX = (double) originalWidth / processedWidth;
            double scaleY = (double) originalHeight / processedHeight;

            CvResult result = new CvResult();
            result.setOriginalWidth(originalWidth);
            result.setOriginalHeight(originalHeight);
            result.setProcessedWidth(processedWidth);
            result.setProcessedHeight(processedHeight);
            result.setStrategy(grid ? "grid" : "auto");
            result.setContourCount(detection.contourCount);
            result.setCandidateCount(detection.candidateCount);

            List<CvTray> trays = new ArrayList<CvTray>();
            int index = 0;
            for (Rect r : rects) {
                Mat trayBgr = bgr.apply(r).clone();
                Mat trayLab = lab.apply(r).clone();
                FillRatio fr = computeFillRatio(trayBgr, trayLab);
                trayBgr.release();
                trayLab.release();

                CvTray tray = new CvTray();
                tray.setIndex(++index);
                tray.setX((int) Math.round(r.x() * scaleX));
                tray.setY((int) Math.round(r.y() * scaleY));
                tray.setWidth((int) Math.round(r.width() * scaleX));
                tray.setHeight((int) Math.round(r.height() * scaleY));
                tray.setFillRatio(fr.ratio);
                tray.setLevel(levelFor(fr.ratio));
                tray.setFoodPixels(fr.food);
                tray.setEmptyPixels(fr.empty);
                tray.setUnknownPixels(fr.unknown);
                trays.add(tray);
            }
            result.setTrays(trays);
            result.setTrayCount(trays.size());

            annotated = bgr.clone();
            drawOverlay(annotated, rects, trays);
            BufferedImage annotatedImage = toBufferedImage(annotated);
            result.setAnnotatedBase64(Base64.getEncoder().encodeToString(encodeJpeg(annotatedImage)));

            return result;
        } finally {
            releaseQuietly(annotated, lab, bgr);
        }
    }

    /** BufferedImage(RGB) -> OpenCV 3 通道 BGR Mat（手动转换，避免 JavaCV 通道顺序歧义）。 */
    private Mat toBgrMat(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        Mat bgr = new Mat(h, w, opencv_core.CV_8UC3);
        for (int y = 0; y < h; y++) {
            BytePointer row = bgr.ptr(y);
            for (int x = 0; x < w; x++) {
                int rgb = image.getRGB(x, y);
                int b = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int idx = x * 3;
                row.put(idx, (byte) b);
                row.put(idx + 1, (byte) g);
                row.put(idx + 2, (byte) r);
            }
        }
        return bgr;
    }

    /** OpenCV 3 通道 BGR Mat -> BufferedImage(RGB)。 */
    private BufferedImage toBufferedImage(Mat bgr) {
        int w = bgr.cols();
        int h = bgr.rows();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            BytePointer row = bgr.ptr(y);
            for (int x = 0; x < w; x++) {
                int idx = x * 3;
                int b = row.get(idx) & 0xFF;
                int g = row.get(idx + 1) & 0xFF;
                int r = row.get(idx + 2) & 0xFF;
                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }

    // ---------------- 原生库加载 ----------------

    private synchronized void ensureNativeLoaded() {
        if (nativeLoaded) {
            return;
        }
        try {
            Loader.load(opencv_core.class);
            Loader.load(opencv_imgproc.class);
            nativeLoaded = true;
        } catch (Throwable t) {
            throw new CvException("OpenCV 原生库加载失败，请确认 pom.xml 已加入对应平台的 opencv/openblas 依赖", t);
        }
    }

    // ---------------- 图片预处理 ----------------

    private BufferedImage toIntRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private BufferedImage scaleToMaxSide(BufferedImage src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        int max = Math.max(w, h);
        if (maxSide <= 0 || max <= maxSide) {
            return src;
        }
        double ratio = (double) maxSide / max;
        int newW = Math.max(1, (int) Math.round(w * ratio));
        int newH = Math.max(1, (int) Math.round(h * ratio));
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return out;
    }

    // ---------------- 餐盘检测 ----------------

    private List<Rect> detectGrid(int width, int height, int rows, int cols) {
        List<Rect> rects = new ArrayList<Rect>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = (int) Math.round((double) c * width / cols);
                int y = (int) Math.round((double) r * height / rows);
                int x2 = (int) Math.round((double) (c + 1) * width / cols);
                int y2 = (int) Math.round((double) (r + 1) * height / rows);
                rects.add(new Rect(x, y, Math.max(1, x2 - x), Math.max(1, y2 - y)));
            }
        }
        return rects;
    }

    private Detection detectAuto(Mat bgr) {
        int width = bgr.cols();
        int height = bgr.rows();

        Mat gray = new Mat();
        Mat blur = new Mat();
        Mat bin = new Mat();
        Mat kernel = null;
        Mat closed = new Mat();
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        try {
            opencv_imgproc.cvtColor(bgr, gray, opencv_imgproc.COLOR_BGR2GRAY);
            int blurK = oddAtLeast(cv.getBlurKernel(), 1);
            opencv_imgproc.GaussianBlur(gray, blur, new Size(blurK, blurK), 0);

            int block = oddAtLeast(cv.getAdaptiveBlockSize(), 3);
            opencv_imgproc.adaptiveThreshold(blur, bin, 255,
                    opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, opencv_imgproc.THRESH_BINARY_INV,
                    block, cv.getAdaptiveC());

            int mk = oddAtLeast(cv.getMorphKernel(), 1);
            kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(mk, mk));
            opencv_imgproc.morphologyEx(bin, closed, opencv_imgproc.MORPH_CLOSE, kernel);

            opencv_imgproc.findContours(closed, contours, opencv_imgproc.RETR_EXTERNAL,
                    opencv_imgproc.CHAIN_APPROX_SIMPLE);

            double maxArea = cv.getMaxAreaRatio() * width * height;
            List<Rect> rects = new ArrayList<Rect>();
            int candidates = 0;
            long n = contours.size();
            for (long i = 0; i < n; i++) {
                Mat contour = contours.get(i);
                double area = opencv_imgproc.contourArea(contour);
                if (area < cv.getMinArea() || area > maxArea) {
                    continue;
                }
                Rect rect = opencv_imgproc.boundingRect(contour);
                if (rect.width() < cv.getMinSide() || rect.height() < cv.getMinSide()) {
                    continue;
                }
                double aspect = (double) rect.width() / rect.height();
                if (aspect < cv.getMinAspectRatio() || aspect > cv.getMaxAspectRatio()) {
                    continue;
                }
                double boxFill = area / rect.area();
                if (boxFill < cv.getMinBoxFill()) {
                    continue;
                }
                // 贴边保护：紧贴图片边缘的框通常是柜子边缘/阴影，不是餐盘
                if (cv.getBorderMargin() > 0) {
                    int m = cv.getBorderMargin();
                    if (rect.x() < m || rect.y() < m
                            || rect.x() + rect.width() > width - m
                            || rect.y() + rect.height() > height - m) {
                        continue;
                    }
                }
                candidates++;
                if (!overlapsAny(rects, rect)) {
                    rects.add(rect);
                }
            }
            sortRects(rects);
            return new Detection(rects, (int) n, candidates);
        } finally {
            releaseQuietly(hierarchy, closed, kernel, bin, blur, gray);
        }
    }

    private boolean overlapsAny(List<Rect> rects, Rect candidate) {
        for (Rect r : rects) {
            double inter = intersectionArea(r, candidate);
            if (inter <= 0) {
                continue;
            }
            double union = (double) r.area() + candidate.area() - inter;
            if (inter / union > 0.35) {
                return true;
            }
        }
        return false;
    }

    private double intersectionArea(Rect a, Rect b) {
        int x1 = Math.max(a.x(), b.x());
        int y1 = Math.max(a.y(), b.y());
        int x2 = Math.min(a.x() + a.width(), b.x() + b.width());
        int y2 = Math.min(a.y() + a.height(), b.y() + b.height());
        if (x2 <= x1 || y2 <= y1) {
            return 0;
        }
        return (double) (x2 - x1) * (y2 - y1);
    }

    private void sortRects(List<Rect> rects) {
        Collections.sort(rects, new Comparator<Rect>() {
            @Override
            public int compare(Rect a, Rect b) {
                int ya = a.y();
                int yb = b.y();
                if (Math.abs(ya - yb) > 10) {
                    return Integer.compare(ya, yb);
                }
                return Integer.compare(a.x(), b.x());
            }
        });
    }

    // ---------------- 剩余占比计算 ----------------

    /**
     * 计算单个餐盘的剩余占比：食物像素 / (食物像素 + 空盘像素)。
     */
    FillRatio computeFillRatio(Mat trayBgr, Mat trayLab) {
        FillRatio result = new FillRatio();
        int w = trayBgr.cols();
        int h = trayBgr.rows();
        if (w < 12 || h < 12) {
            return result;
        }

        int side = Math.min(w, h);
        int bw = clamp((int) Math.round(side * cv.getBorderRatio()), 2, side / 3);
        int inset = clamp((int) Math.round(side * cv.getInsetRatio()), 2, side / 3);

        List<Integer> ls = new ArrayList<Integer>();
        List<Integer> as = new ArrayList<Integer>();
        List<Integer> bs = new ArrayList<Integer>();

        // 上下两条边
        for (int y = 0; y < bw; y++) {
            for (int x = 0; x < w; x++) {
                addLab(trayLab, x, y, ls, as, bs);
            }
        }
        for (int y = h - bw; y < h; y++) {
            for (int x = 0; x < w; x++) {
                addLab(trayLab, x, y, ls, as, bs);
            }
        }
        // 左右两条边（去掉四角重复区域）
        for (int y = bw; y < h - bw; y++) {
            for (int x = 0; x < bw; x++) {
                addLab(trayLab, x, y, ls, as, bs);
            }
            for (int x = w - bw; x < w; x++) {
                addLab(trayLab, x, y, ls, as, bs);
            }
        }

        if (ls.isEmpty()) {
            // 极小餐盘兜底：用整盘中位数当背景
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    addLab(trayLab, x, y, ls, as, bs);
                }
            }
        }

        double bgL = median(ls);
        double bgA = median(as);
        double bgB = median(bs);

        int food = 0;
        int empty = 0;
        int unknown = 0;
        double threshold = cv.getColorDistanceThreshold();

        for (int y = inset; y < h - inset; y++) {
            BytePointer lp = trayLab.ptr(y);
            BytePointer bp = trayBgr.ptr(y);
            for (int x = inset; x < w - inset; x++) {
                int idx3 = x * 3;
                int L = lp.get(idx3) & 0xFF;
                int A = lp.get(idx3 + 1) & 0xFF;
                int B = lp.get(idx3 + 2) & 0xFF;

                if (cv.isIgnoreHighlights()) {
                    int b = bp.get(idx3) & 0xFF;
                    int g = bp.get(idx3 + 1) & 0xFF;
                    int rr = bp.get(idx3 + 2) & 0xFF;
                    // 高光/反光通常接近白色（RGB 三通道都亮）；纯红等饱和色不算高光
                    int mn = Math.min(b, Math.min(g, rr));
                    if (mn > cv.getHighlightValue()) {
                        unknown++;
                        continue;
                    }
                }

                double dl = L - bgL;
                double da = A - bgA;
                double db = B - bgB;
                double dist = Math.sqrt(dl * dl + da * da + db * db);
                if (dist > threshold) {
                    food++;
                } else {
                    empty++;
                }
            }
        }

        int denom = food + empty;
        if (denom > 0) {
            result.ratio = (int) Math.round(food * 100.0 / denom);
        }

        result.food = food;
        result.empty = empty;
        result.unknown = unknown;
        return result;
    }

    private void addLab(Mat lab, int x, int y, List<Integer> ls, List<Integer> as, List<Integer> bs) {
        int idx = x * 3;
        BytePointer p = lab.ptr(y);
        ls.add(p.get(idx) & 0xFF);
        as.add(p.get(idx + 1) & 0xFF);
        bs.add(p.get(idx + 2) & 0xFF);
    }

    private double median(List<Integer> values) {
        Collections.sort(values);
        int n = values.size();
        if (n == 0) {
            return 0;
        }
        if (n % 2 == 1) {
            return values.get(n / 2);
        }
        return (values.get(n / 2 - 1) + values.get(n / 2)) / 2.0;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int oddAtLeast(int value, int min) {
        int v = value;
        if (v % 2 == 0) {
            v++;
        }
        return Math.max(min, v);
    }

    // ---------------- 标注与结果 ----------------

    private void drawOverlay(Mat image, List<Rect> rects, List<CvTray> trays) {
        for (int i = 0; i < rects.size() && i < trays.size(); i++) {
            Rect r = rects.get(i);
            CvTray t = trays.get(i);
            Scalar color = colorFor(t.getFillRatio());
            opencv_imgproc.rectangle(image,
                    new Point(r.x(), r.y()),
                    new Point(r.x() + r.width(), r.y() + r.height()),
                    color, 3, opencv_imgproc.LINE_8, 0);
            String label = "T" + t.getIndex() + " " + (t.getFillRatio() >= 0 ? t.getFillRatio() + "%" : "?");
            opencv_imgproc.putText(image, label,
                    new Point(r.x(), Math.max(14, r.y() - 6)),
                    opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.55, color, 2, opencv_imgproc.LINE_8, false);
        }
    }

    private Scalar colorFor(int ratio) {
        if (ratio < 0) {
            return new Scalar(128, 128, 128, 0);
        }
        ArkProperties.RatioLevel level = ark.getRatioLevel();
        if (ratio >= level.getSufficient()) {
            return new Scalar(0, 255, 0, 0);
        }
        if (ratio >= level.getModerate()) {
            return new Scalar(0, 255, 255, 0);
        }
        return new Scalar(0, 0, 255, 0);
    }

    private String levelFor(int ratio) {
        if (ratio < 0) {
            return LEVEL_UNKNOWN;
        }
        ArkProperties.RatioLevel level = ark.getRatioLevel();
        if (ratio >= level.getSufficient()) {
            return LEVEL_SUFFICIENT;
        }
        if (ratio >= level.getModerate()) {
            return LEVEL_MODERATE;
        }
        if (ratio >= level.getLow()) {
            return LEVEL_LOW;
        }
        return LEVEL_CRITICAL;
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "jpg", out)) {
            throw new IOException("当前环境缺少 JPEG 编码器");
        }
        return out.toByteArray();
    }

    private void releaseQuietly(Mat... mats) {
        for (Mat m : mats) {
            if (m != null) {
                try {
                    m.release();
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
    }

    /** 检测阶段结果：最终餐盘矩形 + 诊断计数。 */
    static class Detection {
        List<Rect> rects;
        int contourCount;
        int candidateCount;

        Detection(List<Rect> rects, int contourCount, int candidateCount) {
            this.rects = rects;
            this.contourCount = contourCount;
            this.candidateCount = candidateCount;
        }
    }

    /** 单个餐盘占比计算结果。 */
    static class FillRatio {
        int ratio = -1;
        int food = 0;
        int empty = 0;
        int unknown = 0;
    }
}
