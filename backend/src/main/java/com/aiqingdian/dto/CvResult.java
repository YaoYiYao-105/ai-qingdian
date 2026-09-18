package com.aiqingdian.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenCV 路线整体结果。
 */
public class CvResult {

    /** 检测到的餐盘数量 */
    private int trayCount;

    private List<CvTray> trays = new ArrayList<CvTray>();

    private int originalWidth;
    private int originalHeight;

    /** 实际处理分辨率（可能被缩小） */
    private int processedWidth;
    private int processedHeight;

    /** 检测策略：auto=轮廓检测；grid=等分网格 */
    private String strategy;

    /** findContours 返回的原始轮廓数量（仅 auto 模式有意义） */
    private int contourCount;

    /** 通过面积/宽高比等过滤后的候选框数量（去重前） */
    private int candidateCount;

    /** 标注后的 JPEG 图（base64，便于直接查看检测框与占比） */
    private String annotatedBase64;

    public int getTrayCount() { return trayCount; }
    public void setTrayCount(int trayCount) { this.trayCount = trayCount; }

    public List<CvTray> getTrays() { return trays; }
    public void setTrays(List<CvTray> trays) { this.trays = trays; }

    public int getOriginalWidth() { return originalWidth; }
    public void setOriginalWidth(int originalWidth) { this.originalWidth = originalWidth; }

    public int getOriginalHeight() { return originalHeight; }
    public void setOriginalHeight(int originalHeight) { this.originalHeight = originalHeight; }

    public int getProcessedWidth() { return processedWidth; }
    public void setProcessedWidth(int processedWidth) { this.processedWidth = processedWidth; }

    public int getProcessedHeight() { return processedHeight; }
    public void setProcessedHeight(int processedHeight) { this.processedHeight = processedHeight; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public int getContourCount() { return contourCount; }
    public void setContourCount(int contourCount) { this.contourCount = contourCount; }

    public int getCandidateCount() { return candidateCount; }
    public void setCandidateCount(int candidateCount) { this.candidateCount = candidateCount; }

    public String getAnnotatedBase64() { return annotatedBase64; }
    public void setAnnotatedBase64(String annotatedBase64) { this.annotatedBase64 = annotatedBase64; }
}
