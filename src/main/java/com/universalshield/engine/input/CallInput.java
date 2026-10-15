package com.universalshield.engine.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The user's own description of a phone call. UniversalShield never records calls and has
 * no access to live caller-ID data.
 *
 * @param callerClaimedIdentity who the caller said they were (e.g. "Bank fraud team")
 * @param callerNumber          number shown on screen, optional, as entered by the user
 * @param description           what was said or asked
 * @param durationMinutes       approximate length, optional
 */
public record CallInput(
        @Size(max = 120, message = "Claimed identity must be at most 120 characters") String callerClaimedIdentity,
        @Pattern(regexp = "^$|^[+0-9 ()\\-]{3,25}$", message = "Caller number may contain only digits, spaces, +, - and brackets")
        String callerNumber,
        @NotBlank(message = "Call description is required")
        @Size(max = 4000, message = "Description must be at most 4000 characters") String description,
        @Min(value = 0, message = "Duration cannot be negative")
        @Max(value = 600, message = "Duration must be at most 600 minutes") Integer durationMinutes) {
}
