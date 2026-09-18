package com.aiqingdian.controller;

import com.aiqingdian.dto.ApiResponse;
import com.aiqingdian.dto.CvResult;
import com.aiqingdian.exception.BadRequestException;
import com.aiqingdian.service.FillRatioService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * OpenCV 路线测试接口：只做餐盘检测与剩余占比计算，不调用豆包模型。
 */
@RestController
@RequestMapping("/api")
public class FillRatioController {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/bmp", "image/gif"
    );

    private final FillRatioService fillRatioService;

    public FillRatioController(FillRatioService fillRatioService) {
        this.fillRatioService = fillRatioService;
    }

    /**
     * @param file 图片文件
     * @param grid 可选：等分网格模式，例如 grid=3x5 表示 3 行 5 列；不传则自动检测餐盘
     */
    @PostMapping(value = "/analyze-cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CvResult> analyzeCv(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "grid", required = false) String grid) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请上传图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("不支持的图片格式，请上传 jpg/png/bmp/gif 图片");
        }

        Integer rows = null;
        Integer cols = null;
        if (grid != null && !grid.trim().isEmpty()) {
            String[] parts = grid.trim().split("[xX*]");
            if (parts.length == 2) {
                try {
                    rows = Integer.valueOf(parts[0].trim());
                    cols = Integer.valueOf(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    rows = null;
                    cols = null;
                }
            }
        }
        CvResult result = fillRatioService.analyze(file.getBytes(), rows, cols);
        return ApiResponse.ok(result);
    }
}
