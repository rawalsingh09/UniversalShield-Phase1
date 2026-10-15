package com.universalshield.security;

import com.universalshield.config.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Keyed HMAC-SHA256 of identifiers such as a recipient, domain, phone number or IP address.
 * Lets the app recognise "you scanned this before" without storing the identifier itself.
 */
@Component
public class SubjectHasher {

    private final byte[] key;

    public SubjectHasher(AppProperties props) {
        this.key = props.security().hashSecret().getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }
}
