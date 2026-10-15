package com.universalshield.detectors.common;

import java.util.regex.Pattern;

/**
 * A reusable text pattern describing one kind of manipulation tactic.
 *
 * @param code           stable id
 * @param group          correlation group used by the scorer
 * @param title          short name shown to users
 * @param explanation    why the tactic matters
 * @param defaultWeight  weight unless a detector overrides it
 * @param pattern        case-insensitive regular expression
 * @param negatable      if true, a match preceded by "not"/"never"/"don't" is ignored
 *                       (so "Never share this OTP" does not count as an OTP request)
 */
public record TextSignal(String code, String group, String title, String explanation,
                         double defaultWeight, Pattern pattern, boolean negatable) {
}
