package com.shiftly.global.response;

import com.shiftly.global.exception.ErrorCode;

/**
 * 모든 API 의 공통 응답 포맷.
 * 성공: { success: true, data: {...}, error: null }
 * 실패: { success: false, data: null, error: { code, message, details } }
 */
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    public record ErrorBody(String code, String message, Object details) {}

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode code) {
        return error(code, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode code, Object details) {
        return new ApiResponse<>(false, null, new ErrorBody(code.name(), code.getMessage(), details));
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message, null));
    }
}
