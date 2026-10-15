package com.universalshield.detectors.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts permission names from an uploaded permission report. Supported formats:
 * <ul>
 *   <li>JSON: {"appName": "...", "permissions": ["android.permission.READ_SMS", ...]}</li>
 *   <li>Text output of {@code aapt dump permissions} / {@code adb shell dumpsys package}: any
 *       {@code android.permission.X} token</li>
 *   <li>Plain list: one permission per line or comma-separated (READ_SMS, CAMERA)</li>
 * </ul>
 * The file content is parsed in memory and never stored.
 */
@Component
public class PermissionReportParser {

    public static final int MAX_PERMISSIONS = 200;
    private static final Pattern ANDROID_PERMISSION = Pattern.compile("android\\.permission\\.([A-Z][A-Z0-9_]{2,80})");
    private static final Pattern BARE_NAME = Pattern.compile("^[A-Z][A-Z0-9_]{2,80}$");

    private final ObjectMapper mapper;

    public PermissionReportParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public record ParsedReport(String appName, List<String> permissions) {
    }

    public ParsedReport parse(String content) {
        String trimmed = content.strip();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode root = mapper.readTree(trimmed);
                Set<String> perms = new LinkedHashSet<>();
                JsonNode arr = root.path("permissions");
                if (arr.isArray()) {
                    for (JsonNode n : arr) {
                        if (n.isTextual() && perms.size() < MAX_PERMISSIONS) {
                            String norm = PermissionCatalog.normalize(n.asText());
                            if (BARE_NAME.matcher(norm).matches()) {
                                perms.add(norm);
                            }
                        }
                    }
                }
                String name = root.path("appName").isTextual() ? truncate(root.path("appName").asText(), 120) : null;
                return new ParsedReport(name, new ArrayList<>(perms));
            } catch (Exception e) {
                throw new IllegalArgumentException("The JSON report could not be parsed.");
            }
        }
        Set<String> perms = new LinkedHashSet<>();
        Matcher m = ANDROID_PERMISSION.matcher(trimmed);
        while (m.find() && perms.size() < MAX_PERMISSIONS) {
            perms.add(m.group(1));
        }
        if (perms.isEmpty()) {
            for (String token : trimmed.split("[,\\r\\n]+")) {
                String t = token.strip().toUpperCase();
                if (BARE_NAME.matcher(t).matches() && perms.size() < MAX_PERMISSIONS) {
                    perms.add(t);
                }
            }
        }
        return new ParsedReport(null, new ArrayList<>(perms));
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
