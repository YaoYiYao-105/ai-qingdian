package com.aiqingdian.dto;

import java.util.List;

/**
 * 门店商品清单条目。
 */
public class CatalogProduct {

    /** 官方名称 */
    private String name;

    /** 别名（模型常见叫法） */
    private List<String> aliases;

    /** 外观特征（可选，帮助模型区分） */
    private String appearance;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public String getAppearance() {
        return appearance;
    }

    public void setAppearance(String appearance) {
        this.appearance = appearance;
    }
}
