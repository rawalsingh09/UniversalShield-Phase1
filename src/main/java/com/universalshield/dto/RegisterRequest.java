package com.universalshield.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email is too long") String email,
        @NotBlank(message = "Name is required") @Size(min = 2, max = 80, message = "Name must be 2-80 characters")
        String displayName,
        @NotBlank(message = "Password is required") @Size(min = 10, max = 128, message = "Password must be 10-128 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one digit")
        String password) {

    /** Keeps the password out of logs and debugger output. */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", displayName=" + displayName + ", password=***]";
    }
}
