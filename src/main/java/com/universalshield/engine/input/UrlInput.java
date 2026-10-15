package com.universalshield.engine.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A URL to analyze structurally. The server never fetches it. */
public record UrlInput(
        @NotBlank(message = "URL is required")
        @Size(max = 2048, message = "URL must be at most 2048 characters") String url) {
}
