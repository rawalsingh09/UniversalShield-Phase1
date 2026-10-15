package com.universalshield.dto;

import java.util.List;

/** Public description of a detection module and whether it is currently operational. */
public record DetectorInfo(String id, String displayName, String version, List<String> categories,
                           String sourceType, boolean available, String description) {
}
