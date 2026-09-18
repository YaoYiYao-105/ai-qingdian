package com.aiqingdian.controller;

import com.aiqingdian.dto.AnalyzeData;
import com.aiqingdian.dto.ApiResponse;
import com.aiqingdian.dto.CatalogProduct;
import com.aiqingdian.exception.BadRequestException;
import com.aiqingdian.service.AnalyzeService;
import com.aiqingdian.service.ProductCatalogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/bmp", "image/gif"
    );

    private final AnalyzeService analyzeService;
    private final ProductCatalogService catalogService;

    public AnalyzeController(AnalyzeService analyzeService, ProductCatalogService catalogService) {
        this.analyzeService = analyzeService;
        this.catalogService = catalogService;
    }

    /**
     * 上传展示柜照片，返回各餐盘剩余占比、充足度、订货合理性与补货建议。
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AnalyzeData> analyze(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请上传图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("不支持的图片格式，请上传 jpg/png/bmp/gif 图片");
        }
        AnalyzeData data = analyzeService.analyze(file);
        return ApiResponse.ok(data);
    }

    /**
     * 返回门店商品清单（前端人工确认下拉用）。
     */
    @GetMapping(value = "/products")
    public ApiResponse<List<CatalogProduct>> products() {
        return ApiResponse.ok(catalogService.getProducts());
    }
}
