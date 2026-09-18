package com.aiqingdian.exception;

/**
 * 请求参数不合法。
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
