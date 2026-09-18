package com.aiqingdian.service;

import com.aiqingdian.config.ArkProperties;
import com.aiqingdian.config.CvProperties;
import com.aiqingdian.dto.CvResult;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FillRatioServiceTest {

    private final FillRatioService service = new FillRatioService(new CvProperties(), new ArkProperties());

    /** 灰度餐盘 + 左侧一半红色食物 -> 占比应约 50%。 */
    @Test
    void gridModeComputesFillRatio() throws Exception {
        BufferedImage img = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(180, 180, 180));
        g.fillRect(0, 0, 800, 400);
        g.setColor(new Color(255, 0, 0));
        // 左格：食物占左半边
        g.fillRect(40, 40, 160, 320);
        // 右格：食物约占 90%
        g.fillRect(400 + 40, 40, 288, 320);
        g.dispose();

        CvResult result = service.analyze(toBytes(img), 1, 2);

        assertEquals("grid", result.getStrategy());
        assertEquals(2, result.getTrayCount());
        assertTrue(result.getTrays().get(0).getFillRatio() >= 40, "左格应约 50%");
        assertTrue(result.getTrays().get(0).getFillRatio() <= 60, "左格应约 50%");
        assertTrue(result.getTrays().get(1).getFillRatio() >= 82, "右格应约 90%");
        assertTrue(result.getTrays().get(1).getFillRatio() <= 98, "右格应约 90%");
        assertNotNull(result.getAnnotatedBase64());
        assertTrue(result.getAnnotatedBase64().length() > 0);
    }

    /** 黑背景 + 浅灰餐盘：自动检测应至少找到一个餐盘并给出占比。 */
    @Test
    void autoModeFindsTrayOnSyntheticCabinet() throws Exception {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 800, 600);
        g.setColor(new Color(200, 200, 200));
        g.fillRect(100, 100, 600, 400);
        g.setColor(new Color(255, 0, 0));
        g.fillRect(140, 140, 260, 320);
        g.dispose();

        CvResult result = service.analyze(toBytes(img), null, null);

        assertEquals("auto", result.getStrategy());
        assertTrue(result.getTrayCount() >= 1, "应至少检测到一个餐盘");
        int ratio = result.getTrays().get(0).getFillRatio();
        assertTrue(ratio >= 35 && ratio <= 65, "自动检测的首个餐盘占比应约 50%，实际 " + ratio);
    }

    private byte[] toBytes(BufferedImage img) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, "png", out), "测试图编码失败");
        return out.toByteArray();
    }
}
