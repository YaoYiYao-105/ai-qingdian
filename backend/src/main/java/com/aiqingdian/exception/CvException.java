package com.aiqingdian.exception;

/**
 * OpenCV 图像处理失败。
 */
public class CvException extends RuntimeException {

    public CvException(String message) {
        super(message);
    }

    public CvException(String message, Throwable cause) {
        super(message, cause);
    }
}
