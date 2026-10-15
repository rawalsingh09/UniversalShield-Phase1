package com.universalshield.engine.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * App permission details entered by the user or parsed from an uploaded permission report.
 *
 * @param appName     display name of the app
 * @param appCategory what the app says it does; used to judge whether permissions look justified
 * @param installSource PLAY_STORE, APP_STORE, OFFICIAL_WEBSITE, UNKNOWN_SOURCE
 * @param permissions Android permission names (android.permission.READ_SMS) or short names (READ_SMS)
 */
public record AppPermissionInput(
        @NotBlank(message = "App name is required")
        @Size(max = 120, message = "App name must be at most 120 characters") String appName,
        @NotBlank(message = "App category is required")
        @Pattern(regexp = "MESSAGING|SOCIAL|BANKING_FINANCE|LOAN|GAME|UTILITY|FLASHLIGHT|NAVIGATION|CAMERA_PHOTO|SHOPPING|HEALTH|ACCESSIBILITY|SECURITY|OTHER",
                message = "Unsupported app category") String appCategory,
        @Pattern(regexp = "PLAY_STORE|APP_STORE|OFFICIAL_WEBSITE|UNKNOWN_SOURCE", message = "Unsupported install source")
        String installSource,
        @NotEmpty(message = "At least one permission is required")
        @Size(max = 200, message = "At most 200 permissions are supported")
        List<@NotBlank @Size(max = 120) @Pattern(regexp = "[A-Za-z0-9_.]+", message = "Permission names may contain only letters, digits, dots and underscores") String> permissions) {
}
