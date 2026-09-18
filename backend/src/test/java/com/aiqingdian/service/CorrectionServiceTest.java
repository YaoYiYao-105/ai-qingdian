package com.aiqingdian.service;

import com.aiqingdian.config.ArkProperties;
import com.aiqingdian.config.CorrectionProperties;
import com.aiqingdian.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrectionServiceTest {

    @TempDir
    Path tempDir;

    private CorrectionService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        CorrectionProperties props = new CorrectionProperties();
        props.setStorageDir(tempDir.toString());

        ArkProperties ark = new ArkProperties();
        ark.setModel("doubao-test");

        ProductCatalogService catalog = new ProductCatalogService();
        catalog.load();

        service = new CorrectionService(props, ark, catalog);
    }

    @Test
    void savesImageAndAppendsJsonlRecord() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "abc".getBytes("UTF-8"));

        Map<String, Object> result = service.save(file, sampleJson("酱香鸭头", false, ""));

        Path metadata = tempDir.resolve("corrections.jsonl");
        assertTrue(Files.exists(metadata));
        String line = Files.readAllLines(metadata).get(0);
        JsonNode node = objectMapper.readTree(line);
        assertEquals("酱香鸭头", node.path("corrections").get(0).path("corrected_name").asText());
        assertEquals("模型原始叫法", node.path("items").get(0).path("model_raw_name").asText());
        assertEquals("模型原始叫法", node.path("corrections").get(0).path("model_raw_name").asText());
        assertEquals("doubao-test", node.path("model").asText());
        assertTrue(node.path("image").path("file").asText().endsWith(".jpg"));
        assertTrue(result.containsKey("session_id"));
        assertEquals(1, result.get("corrections_count"));
    }

    @Test
    void reusesImageFileForSameImageHash() throws Exception {
        byte[] image = "same-image".getBytes("UTF-8");
        MockMultipartFile file1 = new MockMultipartFile("file", "a.jpg", "image/jpeg", image);
        MockMultipartFile file2 = new MockMultipartFile("file", "b.png", "image/png", image);

        Map<String, Object> first = service.save(file1, sampleJson("酱香鸭头", false, ""));
        Map<String, Object> second = service.save(file2, sampleJson("老卤鸡爪", false, ""));

        assertEquals(first.get("image_file"), second.get("image_file"));
        assertEquals(2, Files.readAllLines(tempDir.resolve("corrections.jsonl")).size());
    }

    @Test
    void rejectsUnlistedCorrectedName() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "abc".getBytes());
        assertThrows(BadRequestException.class,
                () -> service.save(file, sampleJson("不存在的商品", false, "")));
    }

    @Test
    void acceptsUnclearRowWithoutCorrectedName() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "abc".getBytes());
        Map<String, Object> result = service.save(file, sampleJson("", true, ""));
        assertEquals(1, result.get("corrections_count"));
    }

    @Test
    void rejectsEmptyCorrections() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "abc".getBytes());
        String json = "{\"raw_model_text\":\"RAW\",\"items\":[{\"row\":1,\"recognized_name\":\"a\","
                + "\"selected_name\":\"a\",\"fill_ratio\":80,\"confidence\":\"high\",\"description\":\"\"}],"
                + "\"corrections\":[]}";
        assertThrows(BadRequestException.class, () -> service.save(file, json));
    }

    private String sampleJson(String correctedName, boolean unclear, String description) {
        return "{"
                + "\"raw_model_text\":\"RAW\","
                + "\"items\":[{\"row\":1,\"recognized_name\":\"上层右侧深色卤味\","
                + "\"selected_name\":\"上层右侧深色卤味\",\"model_raw_name\":\"模型原始叫法\","
                + "\"fill_ratio\":70,\"confidence\":\"low\","
                + "\"description\":\"右上角\",\"x\":null,\"y\":null,\"width\":null,\"height\":null}],"
                + "\"corrections\":[{\"row\":1,\"recognized_name\":\"上层右侧深色卤味\","
                + "\"corrected_name\":\"" + correctedName + "\",\"model_raw_name\":\"模型原始叫法\","
                + "\"unclear\":" + unclear
                + ",\"fill_ratio\":70,\"confidence\":\"low\",\"description\":\"" + description + "\"}]"
                + "}";
    }
}
