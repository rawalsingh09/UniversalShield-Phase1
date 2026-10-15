package com.universalshield.service;

import com.universalshield.dto.ScanRequest;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes sensitive values before anything is stored. Detectors analyse the original text in
 * memory; only the sanitised excerpt is persisted, and only if the user allows it.
 * <ul>
 *   <li>Luhn-valid 13-19 digit numbers (card numbers) become [card number removed].</li>
 *   <li>Codes after words like OTP, PIN, CVV, code, password become [code removed].</li>
 *   <li>Any remaining run of 6+ digits is masked.</li>
 * </ul>
 */
@Component
public class InputSanitizer {

    static final int EXCERPT_MAX = 500;
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){12,18}\\d\\b");
    private static final Pattern SECRET_AFTER_KEYWORD = Pattern.compile(
            "(?i)\\b(otp|pin|cvv|cvc|passcode|password|pwd|code)\\b(\\s*(is|:|=|-)?\\s*)(\\S+)");
    private static final Pattern LONG_DIGITS = Pattern.compile("\\d{6,}");

    public String sanitize(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = CARD.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String digits = m.group().replaceAll("[ -]", "");
            m.appendReplacement(sb, luhnValid(digits) ? "[card number removed]" : Matcher.quoteReplacement(m.group()));
        }
        m.appendTail(sb);
        String out = SECRET_AFTER_KEYWORD.matcher(sb.toString()).replaceAll(r ->
                r.group(4).matches(".*\\d.*") || r.group(1).equalsIgnoreCase("password") || r.group(1).equalsIgnoreCase("pwd")
                        ? Matcher.quoteReplacement(r.group(1) + r.group(2) + "[code removed]")
                        : Matcher.quoteReplacement(r.group()));
        return LONG_DIGITS.matcher(out).replaceAll(r -> "•".repeat(Math.min(r.group().length(), 8)));
    }

    /** Builds the short, sanitised excerpt stored with a scan. */
    public String excerpt(ScanRequest req) {
        String raw = switch (req.category()) {
            case MESSAGE -> (req.message().sender() == null || req.message().sender().isBlank() ? ""
                    : "From: " + req.message().sender() + " | ") + req.message().text();
            case URL -> req.url().url();
            case TRANSACTION -> "Amount " + req.transaction().amount().toPlainString()
                    + (req.transaction().currency() == null ? "" : " " + req.transaction().currency())
                    + (req.transaction().recipient() == null ? "" : " to " + req.transaction().recipient())
                    + (req.transaction().description() == null ? "" : " | " + req.transaction().description());
            case CALL -> (req.call().callerClaimedIdentity() == null ? "" : "Caller: " + req.call().callerClaimedIdentity() + " | ")
                    + req.call().description();
            case APP_PERMISSION -> req.app().appName() + " (" + req.app().appCategory() + "): "
                    + req.app().permissions().size() + " permissions";
        };
        String clean = sanitize(raw.replaceAll("\\s+", " ").trim());
        return clean.length() > EXCERPT_MAX ? clean.substring(0, EXCERPT_MAX - 1) + "…" : clean;
    }

    static boolean luhnValid(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }
}
