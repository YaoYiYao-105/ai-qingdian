package com.aiqingdian.service;

import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 图片压缩：把任意支持的图片统一压缩为 JPEG，并限制最长边。
 * 展示柜照片通常很大，压缩后可显著降低 Base64 体积与 API 费用。
 */
@Service
public class ImageCompressor {

    public byte[] compress(byte[] original, int maxSize, int quality) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
        if (image == null) {
            throw new IOException("无法解析该图片，请上传 jpg/png/bmp/gif 格式的图片");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int max = Math.max(width, height);

        // 已经是 JPEG 且最长边未超过限制：说明前端（或调用方）已压缩过，
        // 直接原样返回，避免再做一次"解码-重编码"的无谓开销。
        if (max <= maxSize && isJpeg(original)) {
            return original;
        }

        if (max > maxSize) {
            double ratio = (double) maxSize / max;
            int newWidth = Math.max(1, (int) Math.round(width * ratio));
            int newHeight = Math.max(1, (int) Math.round(height * ratio));
            BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, 0, 0, newWidth, newHeight, null);
            g.dispose();
            image = scaled;
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("当前环境缺少 JPEG 编码器");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(Math.max(0f, Math.min(1f, quality / 100f)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writer.setOutput(new MemoryCacheImageOutputStream(out));
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /** 通过文件头判断是否为 JPEG（FF D8 FF）。 */
    private boolean isJpeg(byte[] data) {
        return data != null && data.length >= 3
                && (data[0] & 0xFF) == 0xFF
                && (data[1] & 0xFF) == 0xD8
                && (data[2] & 0xFF) == 0xFF;
    }
}
