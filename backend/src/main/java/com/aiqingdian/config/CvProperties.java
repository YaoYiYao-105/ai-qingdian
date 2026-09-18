package com.aiqingdian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 路线 A（OpenCV 经典图像处理）配置。
 * 对应 application.yml 中的 cv.* 配置项。
 */
@ConfigurationProperties(prefix = "cv")
public class CvProperties {

    /** 是否启用 OpenCV 计算通道 */
    private boolean enabled = true;

    /** 处理前把图片最长边缩放到该像素值（加速；0 表示不缩放） */
    private int maxSide = 1800;

    /** 高斯模糊核大小（奇数） */
    private int blurKernel = 5;

    /** 自适应阈值 blockSize（奇数，越大越抗噪，越小细节越多） */
    private int adaptiveBlockSize = 41;

    /** 自适应阈值常数 C（越大检测出的边缘越少） */
    private int adaptiveC = 10;

    /** 形态学闭运算核大小 */
    private int morphKernel = 7;

    /** 候选餐盘最小面积（处理分辨率下） */
    private double minArea = 6000;

    /** 候选餐盘最大面积占整图比例 */
    private double maxAreaRatio = 0.55;

    /** 轮廓近似 epsilon 相对周长的比例（暂未用于筛选，保留扩展） */
    private double approxEpsilon = 0.02;

    /** 轮廓面积至少占其外接矩形面积的比例（过滤非矩形轮廓） */
    private double minBoxFill = 0.55;

    /** 餐盘外接矩形最小边长 */
    private int minSide = 40;

    /** 贴边保护：候选框任一边距图片边缘小于该像素时丢弃（过滤柜子边缘/阴影误检） */
    private int borderMargin = 6;

    /** 餐盘宽高比下限 */
    private double minAspectRatio = 0.4;

    /** 餐盘宽高比上限 */
    private double maxAspectRatio = 2.6;

    /** 边框采样厚度占餐盘短边的比例（用于估计餐盘空盘背景色） */
    private double borderRatio = 0.08;

    /** 计算区域向内收缩占短边的比例（避开餐盘边框/背景） */
    private double insetRatio = 0.10;

    /** Lab 色彩距离阈值：像素与空盘背景色距离大于该值视为食物 */
    private double colorDistanceThreshold = 45.0;

    /** 高于该亮度的像素视为高光/反光，不参与占比计算 */
    private int highlightValue = 245;

    /** 是否忽略高光像素 */
    private boolean ignoreHighlights = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxSide() { return maxSide; }
    public void setMaxSide(int maxSide) { this.maxSide = maxSide; }

    public int getBlurKernel() { return blurKernel; }
    public void setBlurKernel(int blurKernel) { this.blurKernel = blurKernel; }

    public int getAdaptiveBlockSize() { return adaptiveBlockSize; }
    public void setAdaptiveBlockSize(int adaptiveBlockSize) { this.adaptiveBlockSize = adaptiveBlockSize; }

    public int getAdaptiveC() { return adaptiveC; }
    public void setAdaptiveC(int adaptiveC) { this.adaptiveC = adaptiveC; }

    public int getMorphKernel() { return morphKernel; }
    public void setMorphKernel(int morphKernel) { this.morphKernel = morphKernel; }

    public double getMinArea() { return minArea; }
    public void setMinArea(double minArea) { this.minArea = minArea; }

    public double getMaxAreaRatio() { return maxAreaRatio; }
    public void setMaxAreaRatio(double maxAreaRatio) { this.maxAreaRatio = maxAreaRatio; }

    public double getApproxEpsilon() { return approxEpsilon; }
    public void setApproxEpsilon(double approxEpsilon) { this.approxEpsilon = approxEpsilon; }

    public double getMinBoxFill() { return minBoxFill; }
    public void setMinBoxFill(double minBoxFill) { this.minBoxFill = minBoxFill; }

    public int getMinSide() { return minSide; }
    public void setMinSide(int minSide) { this.minSide = minSide; }

    public int getBorderMargin() { return borderMargin; }
    public void setBorderMargin(int borderMargin) { this.borderMargin = borderMargin; }

    public double getMinAspectRatio() { return minAspectRatio; }
    public void setMinAspectRatio(double minAspectRatio) { this.minAspectRatio = minAspectRatio; }

    public double getMaxAspectRatio() { return maxAspectRatio; }
    public void setMaxAspectRatio(double maxAspectRatio) { this.maxAspectRatio = maxAspectRatio; }

    public double getBorderRatio() { return borderRatio; }
    public void setBorderRatio(double borderRatio) { this.borderRatio = borderRatio; }

    public double getInsetRatio() { return insetRatio; }
    public void setInsetRatio(double insetRatio) { this.insetRatio = insetRatio; }

    public double getColorDistanceThreshold() { return colorDistanceThreshold; }
    public void setColorDistanceThreshold(double colorDistanceThreshold) { this.colorDistanceThreshold = colorDistanceThreshold; }

    public int getHighlightValue() { return highlightValue; }
    public void setHighlightValue(int highlightValue) { this.highlightValue = highlightValue; }

    public boolean isIgnoreHighlights() { return ignoreHighlights; }
    public void setIgnoreHighlights(boolean ignoreHighlights) { this.ignoreHighlights = ignoreHighlights; }
}
