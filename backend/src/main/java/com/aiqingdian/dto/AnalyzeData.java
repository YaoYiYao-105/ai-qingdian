package com.aiqingdian.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 识别结果数据（餐盘剩余占比 + 充足度判断）。
 */
public class AnalyzeData {

    /** 识别的展示餐盘数量 */
    private int totalTrays;

    /** 整体库存评分 0-100（各餐盘平均剩余占比）；-1 表示无法计算 */
    private int stockScore = -1;

    /** 整体库存水平：充足 / 一般 / 较少 / 严重不足 / 未知 */
    private String stockLevel;

    /** 订货合理性：合理 / 基本合理 / 偏低 / 明显偏低 / 未知 */
    private String orderReasonableness;

    /** 判断结论（后端根据规则生成） */
    private String reason;

    /** 建议补货的商品（剩余较少/严重不足） */
    private List<ItemInfo> restockSuggestions = new ArrayList<ItemInfo>();

    /** 餐盘清单 */
    private List<ItemInfo> items = new ArrayList<ItemInfo>();

    /** 模型原始返回文本（便于排查） */
    private String raw;

    public int getTotalTrays() {
        return totalTrays;
    }

    public void setTotalTrays(int totalTrays) {
        this.totalTrays = totalTrays;
    }

    public int getStockScore() {
        return stockScore;
    }

    public void setStockScore(int stockScore) {
        this.stockScore = stockScore;
    }

    public String getStockLevel() {
        return stockLevel;
    }

    public void setStockLevel(String stockLevel) {
        this.stockLevel = stockLevel;
    }

    public String getOrderReasonableness() {
        return orderReasonableness;
    }

    public void setOrderReasonableness(String orderReasonableness) {
        this.orderReasonableness = orderReasonableness;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<ItemInfo> getRestockSuggestions() {
        return restockSuggestions;
    }

    public void setRestockSuggestions(List<ItemInfo> restockSuggestions) {
        this.restockSuggestions = restockSuggestions;
    }

    public List<ItemInfo> getItems() {
        return items;
    }

    public void setItems(List<ItemInfo> items) {
        this.items = items;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

}
