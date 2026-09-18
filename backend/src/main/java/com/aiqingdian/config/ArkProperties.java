package com.aiqingdian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 火山方舟（豆包视觉模型）配置。
 * 对应 application.yml 中的 ark.* 配置项。
 */
@ConfigurationProperties(prefix = "ark")
public class ArkProperties {

    /** 火山方舟 API Key（可用环境变量 ARK_API_KEY 覆盖） */
    private String apiKey = "YOUR_ARK_API_KEY";

    /** OpenAI 兼容的 chat/completions 地址 */
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";

    /** 模型 ID 或推理接入点 ID（ep-xxx） */
    private String model = "doubao-1-5-vision-pro-32k-250115";

    /** 调 API 前图片最长边压缩到的像素值 */
    private int maxImageSize = 2048;

    /** JPEG 压缩质量 0-100 */
    private int imageQuality = 85;

    /** 模型最大输出 token 数（餐盘多、字段多时需调大，避免 JSON 被截断） */
    private int maxTokens = 4096;

    /** 深度思考模式：disabled=关闭（最快）、auto=模型自动、enabled=强制开启 */
    private String thinkingType = "disabled";

    /** 连接超时（毫秒） */
    private int connectTimeoutMs = 10000;

    /** 读取超时（毫秒） */
    private int readTimeoutMs = 90000;

    /** 剩余占比 → 充足度等级阈值（%） */
    private RatioLevel ratioLevel = new RatioLevel();

    /** 系统提示词（识别粒度在这里调整） */
    private String systemPrompt = "";

    /** 用户提示词 */
    private String userPrompt = "";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(int maxImageSize) {
        this.maxImageSize = maxImageSize;
    }

    public int getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(int imageQuality) {
        this.imageQuality = imageQuality;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getThinkingType() {
        return thinkingType;
    }

    public void setThinkingType(String thinkingType) {
        this.thinkingType = thinkingType;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public RatioLevel getRatioLevel() {
        return ratioLevel;
    }

    public void setRatioLevel(RatioLevel ratioLevel) {
        this.ratioLevel = ratioLevel;
    }

    /** 剩余占比分级阈值。 */
    public static class RatioLevel {
        /** 占比 >= 该值 → 充足 */
        private int sufficient = 70;
        /** 占比 >= 该值 → 一般 */
        private int moderate = 40;
        /** 占比 >= 该值 → 较少 */
        private int low = 15;

        public int getSufficient() { return sufficient; }
        public void setSufficient(int sufficient) { this.sufficient = sufficient; }
        public int getModerate() { return moderate; }
        public void setModerate(int moderate) { this.moderate = moderate; }
        public int getLow() { return low; }
        public void setLow(int low) { this.low = low; }
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }
}
