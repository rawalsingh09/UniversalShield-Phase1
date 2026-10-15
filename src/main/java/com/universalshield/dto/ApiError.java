package com.universalshield.dto;

import java.util.List;

/**
 * Error body. {@code fields} lists validation problems by field name; rejected values are
 * deliberately never echoed back (they could contain sensitive data).
 */
public record ApiError(String code, String message, List<FieldProblem> fields) {

    public record FieldProblem(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
