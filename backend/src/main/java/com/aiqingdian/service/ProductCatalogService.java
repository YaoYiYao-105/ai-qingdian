package com.aiqingdian.service;

import com.aiqingdian.dto.CatalogProduct;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 门店商品清单：名称匹配、双拼拆分检测。
 */
@Service
public class ProductCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<CatalogProduct> products = new ArrayList<CatalogProduct>();

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource("product-catalog.json").getInputStream()) {
            products = objectMapper.readValue(in, new TypeReference<List<CatalogProduct>>() {
            });
            log.info("商品清单加载完成，共 {} 个商品", products.size());
        } catch (IOException e) {
            throw new IllegalStateException("加载 product-catalog.json 失败", e);
        }
    }

    public List<CatalogProduct> getProducts() {
        return products;
    }

    public boolean isEmpty() {
        return products.isEmpty();
    }

    /** 所有官方名称，用顿号连接（注入提示词用）。 */
    public String joinNames() {
        StringBuilder sb = new StringBuilder();
        for (CatalogProduct p : products) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(p.getName());
        }
        return sb.toString();
    }

    /**
     * 找一个商品的最优匹配，优先级：精确名称 &gt; 别名精确 &gt; 包含匹配（取最长名称）。
     * 返回 null 表示清单中没有匹配。
     */
    public CatalogProduct findBestMatch(String raw) {
        if (raw == null) {
            return null;
        }
        String r = raw.trim();
        if (r.isEmpty()) {
            return null;
        }
        // 1. 精确名称
        for (CatalogProduct p : products) {
            if (p.getName().equals(r)) {
                return p;
            }
        }
        // 2. 别名精确
        for (CatalogProduct p : products) {
            for (String a : aliases(p)) {
                if (a.equals(r)) {
                    return p;
                }
            }
        }
        // 3. 包含匹配（双方都至少 2 个字，避免"鸡"匹配到"鸡爪"）
        CatalogProduct best = null;
        for (CatalogProduct p : products) {
            String n = p.getName();
            if (n.length() >= 2 && r.length() >= 2 && (r.contains(n) || n.contains(r))) {
                if (best == null || n.length() > best.getName().length()) {
                    best = p;
                }
            }
        }
        return best;
    }

    /** 找出 raw 中出现的所有已知商品（用于双拼拆分），按在名称中出现的先后排序。 */
    public List<CatalogProduct> findContained(String raw) {
        List<int[]> found = new ArrayList<int[]>();
        if (raw == null) {
            return new ArrayList<CatalogProduct>();
        }
        String r = raw.trim();
        for (int i = 0; i < products.size(); i++) {
            CatalogProduct p = products.get(i);
            String n = p.getName();
            int idx = -1;
            if (n.length() >= 2) {
                idx = r.indexOf(n);
            }
            if (idx < 0) {
                for (String a : aliases(p)) {
                    if (a.length() >= 2) {
                        idx = r.indexOf(a);
                        if (idx >= 0) {
                            break;
                        }
                    }
                }
            }
            if (idx >= 0) {
                found.add(new int[]{idx, i});
            }
        }
        // 按出现位置排序
        java.util.Collections.sort(found, new java.util.Comparator<int[]>() {
            public int compare(int[] o1, int[] o2) {
                if (o1[0] != o2[0]) {
                    return Integer.compare(o1[0], o2[0]);
                }
                return Integer.compare(o1[1], o2[1]);
            }
        });
        List<CatalogProduct> result = new ArrayList<CatalogProduct>();
        for (int[] f : found) {
            result.add(products.get(f[1]));
        }
        return result;
    }

    /** raw 是否能在清单中找到匹配（含包含匹配）。 */
    public boolean isKnown(String raw) {
        return findBestMatch(raw) != null;
    }

    /** raw 是否等于某个商品的正式名称（别名不算）。 */
    public boolean isOfficialName(String raw) {
        if (raw == null) {
            return false;
        }
        String r = raw.trim();
        if (r.isEmpty()) {
            return false;
        }
        for (CatalogProduct p : products) {
            if (p.getName().equals(r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * raw 是否精确等于某个官方名或别名（不含包含匹配）。
     * 用于判断是否需要拆分：组合名（如"白芸豆配凉拌豇豆"）不是精确匹配，应继续走拆分检测。
     */
    public boolean isExactKnown(String raw) {
        if (raw == null) {
            return false;
        }
        String r = raw.trim();
        if (r.isEmpty()) {
            return false;
        }
        for (CatalogProduct p : products) {
            if (p.getName().equals(r)) {
                return true;
            }
            for (String a : aliases(p)) {
                if (a.equals(r)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> aliases(CatalogProduct p) {
        return p.getAliases() == null ? Collections.<String>emptyList() : p.getAliases();
    }
}
