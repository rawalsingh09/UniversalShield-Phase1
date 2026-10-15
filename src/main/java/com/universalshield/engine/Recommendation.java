package com.universalshield.engine;

/**
 * A concrete next step for the user.
 *
 * @param priority 1 = do this first
 */
public record Recommendation(String action, String reason, int priority) {
}
