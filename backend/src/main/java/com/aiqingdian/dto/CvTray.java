package com.aiqingdian.dto;

/**
 * 单个餐盘在 OpenCV 路线下的检测结果。
 */
public class CvTray {

    /** 序号（从上到下、从左到右） */
    private int index;

    /** 在原始图片中的外接矩形坐标 */
    private int x;
    private int y;
    private int width;
    private int height;

    /** 剩余量占比 0-100；-1 表示无法计算 */
    private int fillRatio = -1;

    /** 充足度等级：充足 / 一般 / 较少 / 严重不足 / 未知 */
    private String level;

    /** 食物像素数 */
    private int foodPixels;

    /** 空盘像素数 */
    private int emptyPixels;

    /** 高光/反光等未参与计算的像素数 */
    private int unknownPixels;

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int getFillRatio() { return fillRatio; }
    public void setFillRatio(int fillRatio) { this.fillRatio = fillRatio; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public int getFoodPixels() { return foodPixels; }
    public void setFoodPixels(int foodPixels) { this.foodPixels = foodPixels; }

    public int getEmptyPixels() { return emptyPixels; }
    public void setEmptyPixels(int emptyPixels) { this.emptyPixels = emptyPixels; }

    public int getUnknownPixels() { return unknownPixels; }
    public void setUnknownPixels(int unknownPixels) { this.unknownPixels = unknownPixels; }
}
