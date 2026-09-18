package com.aiqingdian.service;

import com.aiqingdian.dto.CatalogProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCatalogServiceTest {

    private ProductCatalogService service;

    @BeforeEach
    void setUp() {
        service = new ProductCatalogService();
        service.load();
    }

    @Test
    void loadsCatalog() {
        List<CatalogProduct> products = service.getProducts();
        assertEquals(68, products.size());
        assertEquals("荣昌卤鹅", products.get(0).getName());
    }

    @Test
    void matchesExactName() {
        CatalogProduct p = service.findBestMatch("贡菜");
        assertNotNull(p);
        assertEquals("贡菜", p.getName());
    }

    @Test
    void matchesByAlias() {
        CatalogProduct p = service.findBestMatch("卤青毛豆");
        assertNotNull(p);
        assertEquals("麻辣毛豆", p.getName());
    }

    @Test
    void matchesByContains() {
        CatalogProduct p = service.findBestMatch("凉拌贡菜段");
        assertNotNull(p);
        assertEquals("贡菜", p.getName());
    }

    @Test
    void noMatchForUnknown() {
        assertNull(service.findBestMatch("上层左侧深色卤味"));
        assertFalse(service.isKnown("编织篮装深色卤味"));
    }

    @Test
    void findsContainedProductsForSplit() {
        List<CatalogProduct> contained = service.findContained("白芸豆配凉拌豇豆");
        assertEquals(2, contained.size());
        assertTrue(contained.stream().anyMatch(p -> p.getName().equals("芸豆")));
        assertTrue(contained.stream().anyMatch(p -> p.getName().equals("豇豆")));
    }

    @Test
    void knownProductIsNotSplitCandidate() {
        assertTrue(service.isKnown("酱香核桃豆干"));
    }

    @Test
    void officialNameOnlyAcceptsCatalogName() {
        assertTrue(service.isOfficialName("酱香鸭头"));
        // 卤青毛豆只是别名，正式名应为 麻辣毛豆
        assertFalse(service.isOfficialName("卤青毛豆"));
    }
}
