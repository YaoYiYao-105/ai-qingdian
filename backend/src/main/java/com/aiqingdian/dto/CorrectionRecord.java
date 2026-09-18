package com.aiqingdian.dto;

import java.util.List;

/**
 * 落盘到 corrections.jsonl 的一条训练数据记录。
 * 序列化时统一使用 snake_case。
 */
public class CorrectionRecord {

    private String sessionId;
    private String createdAt;
    private CorrectionImage image;
    private String model;
    private String rawModelText;
    private List<CorrectionRequest.CorrectionItem> items;
    private List<CorrectionRequest.CorrectionEntry> corrections;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public CorrectionImage getImage() {
        return image;
    }

    public void setImage(CorrectionImage image) {
        this.image = image;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRawModelText() {
        return rawModelText;
    }

    public void setRawModelText(String rawModelText) {
        this.rawModelText = rawModelText;
    }

    public List<CorrectionRequest.CorrectionItem> getItems() {
        return items;
    }

    public void setItems(List<CorrectionRequest.CorrectionItem> items) {
        this.items = items;
    }

    public List<CorrectionRequest.CorrectionEntry> getCorrections() {
        return corrections;
    }

    public void setCorrections(List<CorrectionRequest.CorrectionEntry> corrections) {
        this.corrections = corrections;
    }

    public static class CorrectionImage {
        private String file;
        private String sha256;

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }
    }
}
