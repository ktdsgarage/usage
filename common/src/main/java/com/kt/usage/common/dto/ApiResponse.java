package com.kt.usage.common.dto;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ApiResponse<T> {
    private final Integer status;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(Integer status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    public static <T> ApiResponse<T> error(Integer status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
