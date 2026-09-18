package com.aiqingdian.exception;

import com.aiqingdian.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.CODE_BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(Exception e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.CODE_BAD_REQUEST, "请上传图片文件（表单字段名为 file）"));
    }

    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleMediaType(Exception e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ApiResponse.CODE_BAD_REQUEST, "请求格式错误，请使用 multipart/form-data 上传图片"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(ApiResponse.CODE_TOO_LARGE, "图片超过大小限制（最大 10MB）"));
    }

    @ExceptionHandler(CvException.class)
    public ResponseEntity<ApiResponse<Void>> handleCv(CvException e) {
        log.error("OpenCV 处理失败", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ApiResponse.CODE_ERROR, e.getMessage()));
    }

    @ExceptionHandler(ArkException.class)
    public ResponseEntity<ApiResponse<Void>> handleArk(ArkException e) {
        log.warn("火山方舟调用失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ApiResponse.CODE_ERROR, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ApiResponse.CODE_ERROR, "服务器内部错误：" + e.getMessage()));
    }
}
