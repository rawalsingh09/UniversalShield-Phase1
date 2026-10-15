package com.universalshield.detectors.url;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal, dependency-free URL decomposition used for structural analysis. It never resolves
 * DNS or opens connections.
 *
 * @param original     the text as entered
 * @param scheme       lower-case scheme, or "" when none was given
 * @param userInfo     text before '@' in the authority, or ""
 * @param host         lower-case host (may contain non-ASCII characters)
 * @param port         explicit port or -1
 * @param pathAndQuery everything after the authority
 */
public record ParsedUrl(String original, String scheme, String userInfo, String host, int port, String pathAndQuery) {

    private static final Pattern WITH_SCHEME = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*)://(.*)$", Pattern.DOTALL);
    private static final Pattern SPECIAL_SCHEME = Pattern.compile("^(javascript|data|vbscript|mailto|tel|file):(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    /** Second-level labels under which registrations happen (small built-in subset of the Public Suffix List). */
    private static final Set<String> SECOND_LEVEL = Set.of("co", "com", "net", "org", "gov", "ac", "edu", "gen", "firm", "ind", "nic", "res");

    public static Optional<ParsedUrl> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String s = input.trim();
        if (s.isEmpty() || s.chars().anyMatch(Character::isWhitespace)) {
            return Optional.empty();
        }
        String scheme = "";
        String rest = s;
        Matcher m = WITH_SCHEME.matcher(s);
        Matcher special = SPECIAL_SCHEME.matcher(s);
        if (m.matches()) {
            scheme = m.group(1).toLowerCase(Locale.ROOT);
            rest = m.group(2);
        } else if (special.matches()) {
            return Optional.of(new ParsedUrl(input, special.group(1).toLowerCase(Locale.ROOT), "", "", -1, special.group(2)));
        }
        int end = indexOfAny(rest, "/?#");
        String authority = end < 0 ? rest : rest.substring(0, end);
        String path = end < 0 ? "" : rest.substring(end);
        String userInfo = "";
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            userInfo = authority.substring(0, at);
            authority = authority.substring(at + 1);
        }
        int port = -1;
        String host = authority;
        if (authority.startsWith("[")) {
            int close = authority.indexOf(']');
            if (close < 0) {
                return Optional.empty();
            }
            host = authority.substring(0, close + 1);
            String after = authority.substring(close + 1);
            if (after.startsWith(":")) {
                port = parsePort(after.substring(1));
            }
        } else {
            int colon = authority.lastIndexOf(':');
            if (colon >= 0) {
                port = parsePort(authority.substring(colon + 1));
                host = authority.substring(0, colon);
            }
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.isEmpty() || port == -2) {
            return Optional.empty();
        }
        return Optional.of(new ParsedUrl(input, scheme, userInfo, host, port, path));
    }

    private static int parsePort(String p) {
        if (p.isEmpty()) {
            return -1;
        }
        if (!p.matches("\\d{1,5}")) {
            return -2;
        }
        int v = Integer.parseInt(p);
        return v > 65535 ? -2 : v;
    }

    private static int indexOfAny(String s, String chars) {
        for (int i = 0; i < s.length(); i++) {
            if (chars.indexOf(s.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }

    public List<String> labels() {
        return List.of(host.split("\\."));
    }

    public boolean isIpLiteral() {
        return host.startsWith("[") || host.matches("\\d{1,3}(\\.\\d{1,3}){3}")
                || host.matches("\\d{8,10}") || host.matches("0x[0-9a-f]{6,8}");
    }

    /** Approximates the registrable domain ("example.co.uk", "example.com"). */
    public String registrableDomain() {
        if (isIpLiteral()) {
            return host;
        }
        List<String> l = labels();
        if (l.size() <= 2) {
            return host;
        }
        String secondLast = l.get(l.size() - 2);
        String last = l.get(l.size() - 1);
        if (last.length() == 2 && SECOND_LEVEL.contains(secondLast)) {
            return String.join(".", l.subList(l.size() - 3, l.size()));
        }
        return String.join(".", l.subList(l.size() - 2, l.size()));
    }

    /** Labels to the left of the registrable domain. */
    public List<String> subdomainLabels() {
        String reg = registrableDomain();
        if (host.equals(reg)) {
            return List.of();
        }
        String sub = host.substring(0, host.length() - reg.length() - 1);
        return List.of(sub.split("\\."));
    }

    /** The registrable label without its suffix, e.g. "paypal-login" for paypal-login.co.uk. */
    public String registrableLabel() {
        String reg = registrableDomain();
        int dot = reg.indexOf('.');
        return dot < 0 ? reg : reg.substring(0, dot);
    }

    public String tld() {
        List<String> l = labels();
        return l.get(l.size() - 1);
    }
}
