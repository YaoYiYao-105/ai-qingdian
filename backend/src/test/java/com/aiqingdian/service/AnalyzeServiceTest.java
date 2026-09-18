package com.aiqingdian.service;

import com.aiqingdian.config.ArkProperties;
import com.aiqingdian.dto.AnalyzeData;
import com.aiqingdian.dto.ItemInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzeServiceTest {

    private final AnalyzeService service = createService();

    private static AnalyzeService createService() {
        ProductCatalogService catalog = new ProductCatalogService();
        catalog.load();
        return new AnalyzeService(null, null, new ArkProperties(), catalog);
    }

    @Test
    void parsesItemsAndComputesLevelsByThreshold() throws Exception {
        String raw = "{\"items\":["
                + "{\"name\":\"卤青毛豆\",\"fill_ratio\":80,\"confidence\":\"high\"},"
                + "{\"name\":\"香辣萝卜丝\",\"fill_ratio\":55,\"confidence\":\"high\"},"
                + "{\"name\":\"清拌海带丝\",\"fill_ratio\":30,\"confidence\":\"medium\"},"
                + "{\"name\":\"凉拌贡菜段\",\"fill_ratio\":10,\"confidence\":\"medium\"}"
                + "],\"summary\":\"四盘卤味\"}";
        AnalyzeData data = service.parseResult(raw);

        assertEquals(4, data.getTotalTrays());
        assertEquals("充足", data.getItems().get(0).getLevel());
        assertEquals("一般", data.getItems().get(1).getLevel());
        assertEquals("较少", data.getItems().get(2).getLevel());
        assertEquals("严重不足", data.getItems().get(3).getLevel());
        assertEquals(80, data.getItems().get(0).getFillRatio());
    }

    @Test
    void computesAggregatesAndSuggestions() throws Exception {
        // 80, 75, 45, 20, 10 -> 平均 46
        String raw = "{\"items\":["
                + "{\"name\":\"a\",\"fill_ratio\":80},"
                + "{\"name\":\"b\",\"fill_ratio\":75},"
                + "{\"name\":\"c\",\"fill_ratio\":45},"
                + "{\"name\":\"d\",\"fill_ratio\":20},"
                + "{\"name\":\"e\",\"fill_ratio\":10}"
                + "]}";
        AnalyzeData data = service.parseResult(raw);

        assertEquals(46, data.getStockScore());
        assertEquals("一般", data.getStockLevel());
        assertEquals("基本合理", data.getOrderReasonableness());
        assertEquals(2, data.getRestockSuggestions().size());
        assertEquals("d", data.getRestockSuggestions().get(0).getName());
        assertEquals("e", data.getRestockSuggestions().get(1).getName());
        assertTrue(data.getReason().contains("5 个陈列餐盘"));
        assertTrue(data.getReason().contains("平均剩余占比约 46%"));
    }

    @Test
    void highAverageMapsToReasonableOrHigh() throws Exception {
        String raw = "{\"items\":[{\"name\":\"a\",\"fill_ratio\":95},{\"name\":\"b\",\"fill_ratio\":90}]}";
        AnalyzeData data = service.parseResult(raw);
        assertEquals("充足", data.getStockLevel());
        assertEquals("偏高", data.getOrderReasonableness());
        assertEquals(93, data.getStockScore());
    }

    @Test
    void criticalAverageMapsToClearlyLow() throws Exception {
        String raw = "{\"items\":[{\"name\":\"a\",\"fill_ratio\":12},{\"name\":\"b\",\"fill_ratio\":5}]}";
        AnalyzeData data = service.parseResult(raw);
        assertEquals("严重不足", data.getStockLevel());
        assertEquals("明显偏低", data.getOrderReasonableness());
        assertEquals(2, data.getRestockSuggestions().size());
    }

    @Test
    void handlesMissingFillRatio() throws Exception {
        String raw = "{\"items\":[{\"name\":\"a\",\"fill_ratio\":80},{\"name\":\"b\"}]}";
        AnalyzeData data = service.parseResult(raw);
        // b 没有 fill_ratio -> 未知，且不参与平均
        assertEquals("未知", data.getItems().get(1).getLevel());
        assertEquals(80, data.getStockScore());
        assertEquals("充足", data.getStockLevel());
    }

    @Test
    void parsesJsonWithMarkdownFence() throws Exception {
        String raw = "好的，这是结果：\n```json\n{\"items\":[{\"name\":\"a\",\"fill_ratio\":90}],\"summary\":\"ok\"}\n```";
        AnalyzeData data = service.parseResult(raw);
        assertEquals(1, data.getTotalTrays());
        assertEquals(90, data.getStockScore());
        assertEquals("充足", data.getStockLevel());
    }

    @Test
    void parsesJsonWithSurroundingText() throws Exception {
        String raw = "识别完成。{\"items\":[{\"name\":\"水\",\"fill_ratio\":60}]}。以上。";
        AnalyzeData data = service.parseResult(raw);
        assertEquals(60, data.getItems().get(0).getFillRatio());
    }

    @Test
    void throwsWhenNoJsonFound() {
        assertThrows(IllegalArgumentException.class, () -> service.parseResult("抱歉，我看不清这张图片。"));
    }

    @Test
    void handlesEmptyItemsGracefully() throws Exception {
        AnalyzeData data = service.parseResult("{\"items\":[],\"summary\":\"未识别到餐盘\"}");
        assertEquals(0, data.getTotalTrays());
        assertEquals("未知", data.getStockLevel());
        assertEquals(-1, data.getStockScore());
        assertTrue(data.getItems().isEmpty());
    }

    @Test
    void mapsNameToCatalogByAlias() throws Exception {
        String raw = "{\"items\":[{\"name\":\"卤青毛豆\",\"fill_ratio\":80}]}";
        AnalyzeData data = service.parseResult(raw);
        ItemInfo item = data.getItems().get(0);
        assertEquals("麻辣毛豆", item.getName());
        assertEquals("卤青毛豆", item.getOriginalName());
        assertEquals("卤青毛豆", item.getModelRawName());
        assertEquals(true, item.getMatched());
        assertEquals("model", item.getSource());
    }

    @Test
    void marksUnmatchedItem() throws Exception {
        String raw = "{\"items\":[{\"name\":\"上层左侧深色卤味\",\"fill_ratio\":70}]}";
        AnalyzeData data = service.parseResult(raw);
        ItemInfo item = data.getItems().get(0);
        assertEquals("上层左侧深色卤味", item.getName());
        assertEquals(false, item.getMatched());
        assertTrue(data.getReason().contains("1 条未匹配商品清单"));
    }

    @Test
    void splitsComboTray() throws Exception {
        String raw = "{\"items\":[{\"name\":\"白芸豆配凉拌豇豆\",\"fill_ratio\":90}]}";
        AnalyzeData data = service.parseResult(raw);
        assertEquals(2, data.getTotalTrays());
        ItemInfo first = data.getItems().get(0);
        ItemInfo second = data.getItems().get(1);
        assertEquals("芸豆", first.getName());
        assertEquals("豇豆", second.getName());
        assertEquals("白芸豆配凉拌豇豆", first.getModelRawName());
        assertEquals("白芸豆配凉拌豇豆", second.getModelRawName());
        assertEquals(true, first.getMatched());
        assertEquals("split", first.getSource());
        assertEquals(90, first.getFillRatio());
        assertTrue(data.getReason().contains("1 个双拼已拆分"));
    }

    @Test
    void keepsKnownNameUnsplit() throws Exception {
        String raw = "{\"items\":[{\"name\":\"酱香核桃豆干\",\"fill_ratio\":85}]}";
        AnalyzeData data = service.parseResult(raw);
        assertEquals(1, data.getTotalTrays());
        assertEquals("酱香核桃豆干", data.getItems().get(0).getName());
        assertEquals(true, data.getItems().get(0).getMatched());
    }
}
