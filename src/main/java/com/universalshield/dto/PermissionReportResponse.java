package com.universalshield.dto;

import java.util.List;

/** Permissions extracted from an uploaded report, for the user to review before scanning. */
public record PermissionReportResponse(String appName, List<String> permissions) {
}
