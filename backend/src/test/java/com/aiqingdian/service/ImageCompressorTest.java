package com.aiqingdian.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageCompressorTest {

    private final ImageCompressor compressor = new ImageCompressor();

    @Test
    void compressesLargeImageToJpegAndLimitsSize() throws IOException {
        // 生成 4000x3000 的测试图（模拟手机拍的展示柜照片，带噪点更接近真实照片）
        BufferedImage big = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = big.createGraphics();
        java.util.Random random = new java.util.Random(42);
        for (int y = 0; y < 3000; y++) {
            for (int x = 0; x < 4000; x++) {
                big.setRGB(x, y, random.nextInt() | 0xFF000000);
            }
        }
        g.dispose();

        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(big, "png", pngOut), "测试图编码失败");

        byte[] compressed = compressor.compress(pngOut.toByteArray(), 2048, 85);

        BufferedImage out = ImageIO.read(new ByteArrayInputStream(compressed));
        assertNotNull(out, "压缩结果应可被解析为图片");
        assertTrue(Math.max(out.getWidth(), out.getHeight()) <= 2048, "最长边应被限制在 2048 内");
        assertEquals(2048, out.getWidth());
        assertEquals(1536, out.getHeight());
        assertTrue(compressed.length < pngOut.size(), "JPEG 压缩后体积应小于原图");
    }

    @Test
    void keepsSmallImageSize() throws IOException {
        BufferedImage small = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        ImageIO.write(small, "png", pngOut);

        byte[] compressed = compressor.compress(pngOut.toByteArray(), 2048, 85);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(compressed));
        assertNotNull(out);
        assertEquals(100, out.getWidth());
        assertEquals(80, out.getHeight());
    }

    @Test
    void skipsReencodeWhenJpegAlreadyWithinLimit() throws IOException {
        BufferedImage small = new BufferedImage(100, 80, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpgOut = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(small, "jpg", jpgOut), "测试图编码失败");
        byte[] original = jpgOut.toByteArray();

        // 最长边 100 <= 2048 且已是 JPEG：应原样返回，不再重编码
        byte[] result = compressor.compress(original, 2048, 85);

        assertArrayEquals(original, result, "已达标的 JPEG 应原样返回");
    }

    @Test
    void rejectsInvalidImage() {
        byte[] junk = "这不是图片".getBytes();
        try {
            compressor.compress(junk, 2048, 85);
            throw new AssertionError("应抛出 IOException");
        } catch (IOException expected) {
            // ok
        }
    }
}
