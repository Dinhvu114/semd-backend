package com.semd.backend.dto.common;

public record BaseResponse<T>(
    int code,
    boolean success,
    String message,
    T data,
    Metadata metadata
) {
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, true, "Thành công", data, null);
    }

    public static <T> BaseResponse<T> success(T data, Metadata metadata) {
        return new BaseResponse<>(200, true, "Thành công", data, metadata);
    }

    public static <T> BaseResponse<T> fail(String message, int code) {
        return new BaseResponse<>(code, false, message, null, null);
    }
}