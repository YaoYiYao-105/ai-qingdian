package com.aiqingdian.exception;

/**
 * 调用火山方舟 API 失败。
 */
public class ArkException extends RuntimeException {

    public ArkException(String message) {
        super(message);
    }

    public ArkException(String message, Throwable cause) {
        super(message, cause);
    }
}
