package com.aiqingdian.dto;

import java.util.List;

/**
 * 前端提交的人工纠错数据（对应 JSON 字段均为 snake_case）。
 * items 是模型完整识别行列表；corrections 只含用户真正修改过或标为“看不清”的行。
 */
public class CorrectionRequest {

    /** 模型原始返回文本 */
    private String rawModelText;

    /** 展示给用户看过的完整识别行列表 */
    private List<CorrectionItem> items;

    /** 用户修改/看不清的行 */
    private List<CorrectionEntry> corrections;

    public String getRawModelText() {
        return rawModelText;
    }

    public void setRawModelText(String rawModelText) {
        this.rawModelText = rawModelText;
    }

    public List<CorrectionItem> getItems() {
        return items;
    }

    public void setItems(List<CorrectionItem> items) {
        this.items = items;
    }

    public List<CorrectionEntry> getCorrections() {
        return corrections;
    }

    public void setCorrections(List<CorrectionEntry> corrections) {
        this.corrections = corrections;
    }

    public static class CorrectionItem {
        private int row;
        private String recognizedName;
        private String selectedName;
        private String modelRawName;
        private Integer fillRatio;
        private String confidence;
        private String description;
        private Integer x;
        private Integer y;
        private Integer width;
        private Integer height;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getRecognizedName() {
            return recognizedName;
        }

        public void setRecognizedName(String recognizedName) {
            this.recognizedName = recognizedName;
        }

        public String getSelectedName() {
            return selectedName;
        }

        public void setSelectedName(String selectedName) {
            this.selectedName = selectedName;
        }

        public String getModelRawName() {
            return modelRawName;
        }

        public void setModelRawName(String modelRawName) {
            this.modelRawName = modelRawName;
        }

        public Integer getFillRatio() {
            return fillRatio;
        }

        public void setFillRatio(Integer fillRatio) {
            this.fillRatio = fillRatio;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getX() {
            return x;
        }

        public void setX(Integer x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }
    }

    public static class CorrectionEntry {
        private int row;
        private String recognizedName;
        private String correctedName;
        private String modelRawName;
        private boolean unclear;
        private Integer fillRatio;
        private String confidence;
        private String description;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getRecognizedName() {
            return recognizedName;
        }

        public void setRecognizedName(String recognizedName) {
            this.recognizedName = recognizedName;
        }

        public String getCorrectedName() {
            return correctedName;
        }

        public void setCorrectedName(String correctedName) {
            this.correctedName = correctedName;
        }

        public String getModelRawName() {
            return modelRawName;
        }

        public void setModelRawName(String modelRawName) {
            this.modelRawName = modelRawName;
        }

        public boolean isUnclear() {
            return unclear;
        }

        public void setUnclear(boolean unclear) {
            this.unclear = unclear;
        }

        public Integer getFillRatio() {
            return fillRatio;
        }

        public void setFillRatio(Integer fillRatio) {
            this.fillRatio = fillRatio;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
