package com.universalshield.detectors.app;

import java.util.Set;

/**
 * Risk knowledge about one Android permission.
 *
 * @param group        correlation group
 * @param weight       weight when the permission does not appear justified
 * @param title        human-readable name
 * @param explanation  why it can be abused
 * @param justifiedFor app categories where the permission is normally expected
 */
public record PermissionRule(String group, double weight, String title, String explanation, Set<String> justifiedFor) {
}
