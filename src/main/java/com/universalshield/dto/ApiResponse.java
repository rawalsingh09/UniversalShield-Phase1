package com.universalshield.dto;

import java.time.Instant;

/** Consistent envelope for every JSON response: {success, data, error, timestamp}. */
public record ApiResponse<T>(boolean success, T data, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static ApiResponse<Void> fail(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }
}
