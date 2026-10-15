package com.universalshield.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PreferencesDto(
        @NotNull(message = "Retention is required") @Min(value = 1, message = "Retention must be at least 1 day")
        @Max(value = 365, message = "Retention must be at most 365 days") Integer retentionDays,
        @NotNull(message = "Choose whether to store input excerpts") Boolean storeInputExcerpts,
        @NotNull(message = "Default category is required")
        @Pattern(regexp = "MESSAGE|URL|TRANSACTION|CALL|APP_PERMISSION", message = "Unknown category") String defaultCategory) {
}
