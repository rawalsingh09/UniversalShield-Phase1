package com.universalshield.dto;

import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.input.AppPermissionInput;
import com.universalshield.engine.input.CallInput;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.engine.input.TransactionInput;
import com.universalshield.engine.input.UrlInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Body of POST /api/scans. Set {@code category} and the one matching input object, e.g.
 * {"category":"URL","url":{"url":"http://example.test"}}.
 */
public record ScanRequest(
        @NotNull(message = "Category is required") ScanCategory category,
        @Valid MessageInput message,
        @Valid UrlInput url,
        @Valid TransactionInput transaction,
        @Valid CallInput call,
        @Valid AppPermissionInput app) {
}
