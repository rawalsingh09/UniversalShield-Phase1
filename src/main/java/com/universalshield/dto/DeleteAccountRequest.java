package com.universalshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Re-authentication is required before an account and all its data are deleted. */
public record DeleteAccountRequest(@NotBlank(message = "Password is required") @Size(max = 128) String password) {

    @Override
    public String toString() {
        return "DeleteAccountRequest[password=***]";
    }
}
