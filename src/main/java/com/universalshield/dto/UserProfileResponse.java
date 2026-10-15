package com.universalshield.dto;

import java.time.Instant;

/** What the API reveals about the signed-in user. Never includes the password hash. */
public record UserProfileResponse(String email, String displayName, Instant memberSince, PreferencesDto preferences) {
}
