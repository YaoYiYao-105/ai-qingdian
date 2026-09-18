package com.aiqingdian.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 训练数据采集（人工纠错）配置。
 * 对应 application.yml 中的 correction.* 配置项。
 */
@ConfigurationProperties(prefix = "correction")
public class CorrectionProperties {

    /** 训练数据根目录：图片文件与 corrections.jsonl 都放在这里 */
    private String storageDir = "data/training-data";

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }
}
