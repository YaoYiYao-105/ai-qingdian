package com.aiqingdian.controller;

import com.aiqingdian.dto.ApiResponse;
import com.aiqingdian.service.CorrectionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 人工纠错训练数据保存接口。
 */
@RestController
@RequestMapping("/api")
public class CorrectionController {

    private final CorrectionService correctionService;

    public CorrectionController(CorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    /**
     * 保存一次纠错：multipart 中 file=原图，json=完整识别结果与修正列表。
     */
    @PostMapping(value = "/corrections", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> save(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("json") String json) throws IOException {
        return ApiResponse.ok(correctionService.save(file, json));
    }
}
