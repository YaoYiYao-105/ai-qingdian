package com.aiqingdian.dto;

/**
 * 单个餐盘商品识别结果。
 */
public class ItemInfo {

    /** 商品名称（已按清单归一化；未匹配则为模型原始描述） */
    private String name;

    /** 模型原始叫法（与 name 不同时返回，便于核对） */
    private String originalName;

    /** 模型对这条餐盘最原始的叫法（未归一化，训练纠错数据用） */
    private String modelRawName;

    /** 餐盘剩余量占比 0-100；-1 表示模型未给出 */
    private int fillRatio = -1;

    /** 充足度等级：充足 / 一般 / 较少 / 严重不足 / 未知 */
    private String level;

    /** 置信度：high / medium / low */
    private String confidence;

    /** 是否匹配到商品清单（false 表示需人工确认） */
    private Boolean matched;

    /** 来源：model=模型识别；split=双拼拆分 */
    private String source;

    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getModelRawName() {
        return modelRawName;
    }

    public void setModelRawName(String modelRawName) {
        this.modelRawName = modelRawName;
    }

    public int getFillRatio() {
        return fillRatio;
    }

    public void setFillRatio(int fillRatio) {
        this.fillRatio = fillRatio;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
