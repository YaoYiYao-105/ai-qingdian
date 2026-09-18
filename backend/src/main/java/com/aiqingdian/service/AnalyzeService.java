package com.aiqingdian.service;

import com.aiqingdian.config.ArkProperties;
import com.aiqingdian.dto.AnalyzeData;
import com.aiqingdian.dto.CatalogProduct;
import com.aiqingdian.dto.ItemInfo;
import com.aiqingdian.exception.ArkException;
import com.aiqingdian.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 识别主流程：接收图片 -> 压缩 -> Base64 -> 调豆包视觉模型 -> 解析餐盘剩余占比 -> 名称归一化/双拼拆分 -> 计算充足度与订货合理性。
 */
@Service
public class AnalyzeService {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeService.class);

    public static final String LEVEL_SUFFICIENT = "充足";
    public static final String LEVEL_MODERATE = "一般";
    public static final String LEVEL_LOW = "较少";
    public static final String LEVEL_CRITICAL = "严重不足";
    public static final String LEVEL_UNKNOWN = "未知";

    public static final String SOURCE_MODEL = "model";
    public static final String SOURCE_SPLIT = "split";

    private final ImageCompressor imageCompressor;
    private final ArkClient arkClient;
    private final ArkProperties properties;
    private final ProductCatalogService catalogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyzeService(ImageCompressor imageCompressor, ArkClient arkClient,
                          ArkProperties properties, ProductCatalogService catalogService) {
        this.imageCompressor = imageCompressor;
        this.arkClient = arkClient;
        this.properties = properties;
        this.catalogService = catalogService;
    }

    public AnalyzeData analyze(MultipartFile file) throws IOException {
        long t0 = System.nanoTime();
        byte[] original = file.getBytes();
        byte[] compressed;
        try {
            compressed = imageCompressor.compress(original, properties.getMaxImageSize(), properties.getImageQuality());
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }
        long t1 = System.nanoTime();
        String base64 = Base64.getEncoder().encodeToString(compressed);

        String systemPrompt = buildSystemPrompt();
        String raw = arkClient.chat(base64, "image/jpeg", systemPrompt, properties.getUserPrompt());
        long t2 = System.nanoTime();
        log.info("模型原始返回：{}", raw);

        try {
            AnalyzeData data = parseResult(raw);
            long t3 = System.nanoTime();
            log.info("识别耗时统计：图片压缩 {}ms，模型调用 {}ms，结果解析 {}ms，总计 {}ms",
                    (t1 - t0) / 1_000_000L, (t2 - t1) / 1_000_000L,
                    (t3 - t2) / 1_000_000L, (t3 - t0) / 1_000_000L);
            return data;
        } catch (Exception e) {
            log.warn("解析模型返回失败：{}", e.getMessage());
            log.warn("模型原始返回(完整)：{}", raw);
            throw new ArkException("模型返回内容无法解析为 JSON，请查看原始文本排查。原始内容：" + abbreviate(raw));
        }
    }

    /** 系统提示词 = 基础提示词 + 门店商品清单（运行时注入，方便更新清单无需改代码）。 */
    private String buildSystemPrompt() {
        String base = properties.getSystemPrompt();
        if (catalogService.isEmpty()) {
            return base;
        }
        return base + "\n\n门店商品清单（名称参照，请优先使用清单中的名称；无法确定对应关系时不要硬选，按上面规则处理）：\n"
                + catalogService.joinNames();
    }

    /**
     * 把模型返回文本解析为 {@link AnalyzeData}：
     * 1) 解析 items；2) 名称归一化（映射到清单）；3) 双拼/拼盘组合名拆分；4) 按阈值计算充足度、库存、订货合理性与补货建议。
     */
    AnalyzeData parseResult(String raw) throws IOException {
        return parseResult(raw, 0, 0);
    }

    AnalyzeData parseResult(String raw, int imageWidth, int imageHeight) throws IOException {
        String json = extractJson(raw);
        JsonNode node = objectMapper.readTree(json);

        AnalyzeData data = new AnalyzeData();
        data.setRaw(raw);

        List<ItemInfo> items = new ArrayList<ItemInfo>();
        JsonNode itemsNode = node.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                ItemInfo info = new ItemInfo();
                String rawName = item.path("name").asText("");
                info.setName(rawName);
                info.setModelRawName(rawName);
                info.setFillRatio(item.path("fill_ratio").asInt(-1));
                info.setConfidence(item.path("confidence").asText("unknown"));
                info.setDescription(item.path("description").asText(""));
                info.setLevel(levelFor(info.getFillRatio()));
                info.setSource(SOURCE_MODEL);
                items.add(info);
            }
        }

        // 名称归一化 + 双拼拆分
        List<ItemInfo> normalized = new ArrayList<ItemInfo>();
        int splitCount = 0;
        for (ItemInfo item : items) {
            String rawName = item.getName();
            if (!catalogService.isExactKnown(rawName)) {
                List<CatalogProduct> contained = catalogService.findContained(rawName);
                if (contained.size() >= 2) {
                    for (CatalogProduct cp : contained) {
                        ItemInfo sub = new ItemInfo();
                        sub.setName(cp.getName());
                        sub.setOriginalName(rawName);
                        sub.setModelRawName(rawName);
                        sub.setFillRatio(item.getFillRatio());
                        sub.setConfidence(item.getConfidence());
                        sub.setDescription(item.getDescription());
                        sub.setLevel(levelFor(item.getFillRatio()));
                        sub.setMatched(true);
                        sub.setSource(SOURCE_SPLIT);
                        normalized.add(sub);
                    }
                    splitCount++;
                    continue;
                }
            }
            CatalogProduct matched = catalogService.findBestMatch(rawName);
            if (matched != null) {
                item.setMatched(true);
                if (!matched.getName().equals(rawName)) {
                    item.setName(matched.getName());
                    item.setOriginalName(rawName);
                }
            } else {
                item.setMatched(false);
            }
            normalized.add(item);
        }
        data.setItems(normalized);
        data.setTotalTrays(normalized.size());

        // 汇总：平均剩余占比 -> 整体库存水平与评分
        int sum = 0;
        int counted = 0;
        for (ItemInfo item : normalized) {
            if (item.getFillRatio() >= 0) {
                sum += item.getFillRatio();
                counted++;
            }
        }
        if (counted > 0) {
            int avg = (int) Math.round((double) sum / counted);
            data.setStockScore(avg);
            data.setStockLevel(levelFor(avg));
        } else {
            data.setStockScore(-1);
            data.setStockLevel(LEVEL_UNKNOWN);
        }
        data.setOrderReasonableness(reasonablenessFor(data.getStockLevel(), data.getStockScore()));

        // 补货建议：剩余较少或严重不足的餐盘
        List<ItemInfo> suggestions = new ArrayList<ItemInfo>();
        for (ItemInfo item : normalized) {
            if (LEVEL_LOW.equals(item.getLevel()) || LEVEL_CRITICAL.equals(item.getLevel())) {
                suggestions.add(item);
            }
        }
        data.setRestockSuggestions(suggestions);

        String modelSummary = node.path("summary").asText("");
        data.setReason(buildReason(data, splitCount, modelSummary));

        return data;
    }


    /** 剩余占比 -> 充足度等级。ratio < 0 视为模型未给出。 */
    String levelFor(int ratio) {
        ArkProperties.RatioLevel threshold = properties.getRatioLevel();
        if (ratio < 0) {
            return LEVEL_UNKNOWN;
        }
        if (ratio >= threshold.getSufficient()) {
            return LEVEL_SUFFICIENT;
        }
        if (ratio >= threshold.getModerate()) {
            return LEVEL_MODERATE;
        }
        if (ratio >= threshold.getLow()) {
            return LEVEL_LOW;
        }
        return LEVEL_CRITICAL;
    }

    /** 整体库存水平 -> 订货合理性结论。 */
    String reasonablenessFor(String level, int score) {
        if (LEVEL_SUFFICIENT.equals(level)) {
            return score >= 90 ? "偏高" : "合理";
        }
        if (LEVEL_MODERATE.equals(level)) {
            return "基本合理";
        }
        if (LEVEL_LOW.equals(level)) {
            return "偏低";
        }
        if (LEVEL_CRITICAL.equals(level)) {
            return "明显偏低";
        }
        return LEVEL_UNKNOWN;
    }

    private String buildReason(AnalyzeData data, int splitCount, String modelSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("共识别 ").append(data.getTotalTrays()).append(" 个陈列餐盘");
        if (splitCount > 0) {
            sb.append("（其中 ").append(splitCount).append(" 个双拼已拆分）");
        }
        if (data.getStockScore() >= 0) {
            sb.append("，平均剩余占比约 ").append(data.getStockScore()).append("%");
        }
        int lowCount = data.getRestockSuggestions().size();
        if (lowCount > 0) {
            sb.append("，其中 ").append(lowCount).append(" 盘剩余偏少");
        }
        int unmatched = 0;
        for (ItemInfo item : data.getItems()) {
            if (Boolean.FALSE.equals(item.getMatched())) {
                unmatched++;
            }
        }
        if (unmatched > 0) {
            sb.append("，").append(unmatched).append(" 条未匹配商品清单，需人工确认");
        }
        sb.append("。整体库存").append(data.getStockLevel())
                .append("，本次订货量").append(data.getOrderReasonableness()).append("。");
        if (modelSummary != null && !modelSummary.trim().isEmpty()) {
            sb.append("模型备注：").append(modelSummary.trim());
        }
        return sb.toString();
    }

    /** 从模型文本中抽取第一个 {...} JSON 片段。 */
    private String extractJson(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("模型返回为空");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException("返回内容中未找到 JSON 对象");
        }
        return raw.substring(start, end + 1);
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }
}
