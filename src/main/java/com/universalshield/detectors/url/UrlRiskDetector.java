package com.universalshield.detectors.url;

import com.universalshield.detectors.common.TextSignals;
import com.universalshield.engine.DetectionModule;
import com.universalshield.engine.DetectorResult;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.Indicator;
import com.universalshield.engine.ResultSource;
import com.universalshield.engine.ScanCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Structural URL analysis. Works on the URL text only: it never resolves, fetches or executes
 * anything, so there is no SSRF exposure. It also checks links embedded in messages, call
 * descriptions and transaction descriptions.
 */
@Component
public class UrlRiskDetector implements DetectionModule {

    public static final String ID = "url-heuristic";
    static final String VERSION = "1.0.0";
    static final int MAX_EMBEDDED_URLS = 5;

    /** Brand token to the registrable domains that genuinely belong to it. */
    static final Map<String, Set<String>> BRANDS = Map.ofEntries(
            Map.entry("paypal", Set.of("paypal.com", "paypal.me")),
            Map.entry("amazon", Set.of("amazon.com", "amazon.in", "amazon.co.uk", "amazon.de")),
            Map.entry("google", Set.of("google.com", "google.co.in", "google.co.uk")),
            Map.entry("microsoft", Set.of("microsoft.com", "live.com", "office.com")),
            Map.entry("apple", Set.of("apple.com", "icloud.com")),
            Map.entry("netflix", Set.of("netflix.com")),
            Map.entry("facebook", Set.of("facebook.com", "fb.com")),
            Map.entry("instagram", Set.of("instagram.com")),
            Map.entry("whatsapp", Set.of("whatsapp.com", "whatsapp.net")),
            Map.entry("linkedin", Set.of("linkedin.com")),
            Map.entry("sbi", Set.of("sbi.co.in", "onlinesbi.sbi", "sbi.bank.in")),
            Map.entry("hdfc", Set.of("hdfcbank.com", "hdfc.com")),
            Map.entry("icici", Set.of("icicibank.com")),
            Map.entry("paytm", Set.of("paytm.com")),
            Map.entry("phonepe", Set.of("phonepe.com")),
            Map.entry("flipkart", Set.of("flipkart.com")));
    static final Set<String> SHORTENERS = Set.of("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd",
            "buff.ly", "cutt.ly", "rebrand.ly", "shorturl.at", "rb.gy", "tiny.cc", "t.ly", "s.id");
    static final Set<String> RISKY_TLDS = Set.of("zip", "mov", "xyz", "top", "click", "gq", "tk", "ml", "cf", "ga",
            "work", "rest", "country", "kim", "loan", "men", "cam", "icu", "sbs", "cfd", "buzz", "monster", "link");
    static final Set<String> LURE_WORDS = Set.of("login", "signin", "sign-in", "verify", "verification", "secure",
            "account", "update", "banking", "wallet", "kyc", "password", "confirm", "unlock", "suspended", "reward", "refund");
    static final Pattern DOWNLOAD_EXT = Pattern.compile("\\.(apk|exe|msi|scr|bat|cmd|ps1|jar|vbs|dmg|iso|xapk)(\\?|#|$)",
            Pattern.CASE_INSENSITIVE);
    static final Pattern EMBEDDED_URL = Pattern.compile(
            "(?i)\\b((?:https?://|www\\.)[^\\s<>\"']+|[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.(?:com|net|org|in|xyz|top|ly|co|info|io|me|zip|click|link|online|site|live|shop)(?:/[^\\s<>\"']*)?)");

    private final DomainReputationProvider reputation;

    public UrlRiskDetector(DomainReputationProvider reputation) {
        this.reputation = reputation;
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "URL & website structure analyzer"; }
    @Override public String version() { return VERSION; }
    @Override public String description() {
        return "Checks URL structure, HTTPS usage, deceptive domains, subdomain abuse and obfuscation. "
                + "Never fetches the URL. Also checks links embedded in messages and descriptions.";
    }
    @Override public Set<ScanCategory> categories() { return Set.of(ScanCategory.URL); }
    @Override public ResultSource sourceType() { return ResultSource.LOCAL_HEURISTIC; }

    @Override
    public boolean appliesTo(FraudEvent event) {
        return event.category() == ScanCategory.URL || !extractUrls(event.freeText()).isEmpty();
    }

    @Override
    public DetectorResult analyze(FraudEvent event) {
        List<Indicator> indicators = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        boolean sufficient = true;
        boolean anyVerified = false;
        if (event.category() == ScanCategory.URL) {
            Optional<ParsedUrl> parsed = ParsedUrl.parse(event.url().url());
            if (parsed.isEmpty()) {
                indicators.add(ind("URL_MALFORMED", "STRUCTURE", "Malformed or unparseable URL",
                        "The address could not be parsed as a normal web address; malformed URLs are sometimes used to confuse users.",
                        0.3, TextSignals.evidence(event.url().url())));
                sufficient = false;
            } else {
                indicators.addAll(analyzeUrl(parsed.get(), ""));
                anyVerified |= addReputation(parsed.get(), "", indicators, notes);
                if (event.history().priorHighRiskOfSubject() > 0) {
                    indicators.add(ind("URL_PREVIOUSLY_HIGH_RISK", "KNOWN_RISKY", "Domain flagged in your earlier scans",
                            "You scanned this domain before and received a HIGH or CRITICAL result.", 0.45,
                            parsed.get().registrableDomain()));
                }
            }
        } else {
            List<String> urls = extractUrls(event.freeText());
            notes.add("Found " + urls.size() + " link(s) in the text; each was analyzed structurally without being opened.");
            for (String u : urls) {
                ParsedUrl.parse(u).ifPresent(p -> indicators.addAll(analyzeUrl(p, "url:")));
            }
        }
        notes.add("HTTPS only means the connection is encrypted. It does not show who runs a site or that it is safe.");
        if (!anyVerified) {
            notes.add("Verified domain reputation: not used. These are heuristic findings only.");
        }
        return new DetectorResult(ID, VERSION, ResultSource.LOCAL_HEURISTIC, indicators, notes, sufficient);
    }

    private boolean addReputation(ParsedUrl url, String prefix, List<Indicator> indicators, List<String> notes) {
        ReputationVerdict v = reputation.lookup(url.registrableDomain());
        switch (v.status()) {
            case MALICIOUS -> indicators.add(new Indicator("URL_REPUTATION_MALICIOUS", prefix + "REPUTATION",
                    "Listed as malicious by " + v.provider(), v.detail(), 0.9, ID, ResultSource.VERIFIED_EXTERNAL,
                    url.registrableDomain()));
            case SUSPICIOUS -> indicators.add(new Indicator("URL_REPUTATION_SUSPICIOUS", prefix + "REPUTATION",
                    "Listed as suspicious by " + v.provider(), v.detail(), 0.5, ID, ResultSource.VERIFIED_EXTERNAL,
                    url.registrableDomain()));
            case CLEAN -> notes.add(v.provider() + " has no listing for this domain. That is not a guarantee of safety.");
            default -> { return false; }
        }
        return true;
    }

    /** Pure structural analysis of one URL. */
    List<Indicator> analyzeUrl(ParsedUrl u, String prefix) {
        List<Indicator> out = new ArrayList<>();
        String host = u.host();
        String full = u.original().toLowerCase(Locale.ROOT);

        switch (u.scheme()) {
            case "javascript", "data", "vbscript" -> {
                out.add(ind("URL_SCRIPT_SCHEME", prefix + "OBFUSCATION", "Script or data URL",
                        "This is not a web address; it can run code or embed content directly when opened.", 0.9, u.scheme() + ":"));
                return out;
            }
            case "file" -> out.add(ind("URL_FILE_SCHEME", prefix + "OBFUSCATION", "Local file link",
                    "File links point at files rather than websites and are unusual in messages.", 0.5, "file:"));
            case "http" -> out.add(ind("URL_NO_HTTPS", prefix + "TRANSPORT", "No HTTPS",
                    "The link uses unencrypted HTTP. Never enter data on such a page (HTTPS alone would not prove safety either).",
                    0.2, "http://"));
            case "https", "", "mailto", "tel" -> { }
            default -> out.add(ind("URL_UNUSUAL_SCHEME", prefix + "STRUCTURE", "Unusual scheme",
                    "The link uses an uncommon protocol.", 0.2, u.scheme()));
        }
        if (host.isEmpty()) {
            return out;
        }
        if (!u.userInfo().isEmpty()) {
            out.add(ind("URL_USERINFO_TRICK", prefix + "OBFUSCATION", "Text before '@' hides the real host",
                    "Browsers ignore everything before '@' in an address, so 'trusted.com@evil.example' really goes to evil.example.",
                    0.6, TextSignals.evidence(u.userInfo() + "@")));
        }
        if (u.isIpLiteral()) {
            out.add(ind("URL_IP_HOST", prefix + "HOST_FORM", "Raw IP address instead of a domain",
                    "Legitimate organisations almost never send links to bare IP addresses.", 0.45, host));
        }
        if (u.port() > 0 && u.port() != 80 && u.port() != 443) {
            out.add(ind("URL_NONSTANDARD_PORT", prefix + "HOST_FORM", "Non-standard port",
                    "The link uses an unusual network port, which is rare for real consumer websites.", 0.2, ":" + u.port()));
        }
        if (host.contains("xn--")) {
            out.add(ind("URL_PUNYCODE", prefix + "DECEPTIVE_DOMAIN", "Punycode (internationalised) domain",
                    "Punycode can display look-alike characters, e.g. a Cyrillic 'а' that looks like a Latin 'a'.", 0.4, host));
        } else if (!host.chars().allMatch(c -> c < 128)) {
            out.add(ind("URL_NON_ASCII_HOST", prefix + "DECEPTIVE_DOMAIN", "Non-ASCII characters in domain",
                    "Characters from other alphabets can imitate familiar letters (homoglyph attack).", 0.45, host));
        }
        if (host.contains("%")) {
            out.add(ind("URL_ENCODED_HOST", prefix + "OBFUSCATION", "Percent-encoded host",
                    "Encoding characters in the host name hides where the link really goes.", 0.5, host));
        }
        if (u.pathAndQuery().split("%[0-9a-fA-F]{2}", -1).length > 7) {
            out.add(ind("URL_HEAVY_ENCODING", prefix + "OBFUSCATION", "Heavily encoded path",
                    "Large amounts of percent-encoding are used to disguise the content of a link.", 0.25, "%xx…"));
        }
        if (u.pathAndQuery().matches("(?i).*[?&](url|redirect|next|goto|continue|dest)=(https?%3a|https?:|//).*")) {
            out.add(ind("URL_OPEN_REDIRECT", prefix + "OBFUSCATION", "Embedded redirect to another site",
                    "The link carries another web address as a parameter and may forward you somewhere else.", 0.3, "redirect="));
        }
        String registrable = u.registrableDomain();
        if (!u.isIpLiteral()) {
            brandChecks(u, prefix, out);
            if (SHORTENERS.contains(registrable)) {
                out.add(ind("URL_SHORTENER", prefix + "SHORTENER", "Link shortener",
                        "Shortened links hide the real destination until you open them.", 0.25, registrable));
            }
            if (RISKY_TLDS.contains(u.tld())) {
                out.add(ind("URL_RISKY_TLD", prefix + "TLD", "Top-level domain often abused",
                        "This ending (." + u.tld() + ") is cheap and frequently used in abuse. Many legitimate sites use it too.",
                        0.25, "." + u.tld()));
            }
            if (u.subdomainLabels().size() >= 3) {
                out.add(ind("URL_DEEP_SUBDOMAINS", prefix + "SUBDOMAIN", "Many subdomain levels",
                        "Long chains of subdomains are used to push the real domain out of view on small screens.", 0.25, host));
            }
            long hyphens = u.registrableLabel().chars().filter(c -> c == '-').count();
            if (hyphens >= 2) {
                out.add(ind("URL_HYPHENATED_DOMAIN", prefix + "STRUCTURE", "Heavily hyphenated domain",
                        "Domains like 'secure-login-update' imitate official wording.", 0.2, registrable));
            }
        }
        Set<String> lure = new LinkedHashSet<>();
        for (String w : LURE_WORDS) {
            if (full.contains(w)) {
                lure.add(w);
            }
        }
        if (!lure.isEmpty()) {
            out.add(ind("URL_LURE_KEYWORDS", prefix + "LURE_KEYWORDS", "Security or account words in the link",
                    "Words like 'verify', 'login' or 'secure' in a link are used to look official.",
                    lure.size() >= 2 ? 0.3 : 0.15, String.join(", ", lure)));
        }
        if (DOWNLOAD_EXT.matcher(u.pathAndQuery()).find()) {
            out.add(ind("URL_EXECUTABLE_DOWNLOAD", prefix + "DOWNLOAD", "Link to an installable file",
                    "The link points to an app or program file (for example .apk or .exe). Installing it could compromise your device.",
                    0.55, TextSignals.evidence(u.pathAndQuery())));
        }
        if (u.original().length() > 120) {
            out.add(ind("URL_VERY_LONG", prefix + "STRUCTURE", "Unusually long URL",
                    "Very long links can hide suspicious parts beyond what is visible.", 0.1, u.original().length() + " characters"));
        }
        return out;
    }

    private void brandChecks(ParsedUrl u, String prefix, List<Indicator> out) {
        String registrable = u.registrableDomain();
        String label = u.registrableLabel();
        for (Map.Entry<String, Set<String>> e : BRANDS.entrySet()) {
            String brand = e.getKey();
            if (e.getValue().contains(registrable)) {
                continue;
            }
            boolean inLabel = List.of(label.split("[-0-9]")).contains(brand);
            boolean inSub = u.subdomainLabels().stream().anyMatch(s -> List.of(s.split("[-0-9]")).contains(brand));
            if (inLabel || inSub) {
                out.add(ind("URL_BRAND_MISUSE", prefix + "DECEPTIVE_DOMAIN", "Brand name on an unrelated domain",
                        "The link mentions '" + brand + "' but the actual domain is " + registrable + ", which does not belong to that brand.",
                        0.55, u.host()));
                return;
            }
            String deleet = deleet(label);
            int dist = levenshtein(deleet, brand);
            // A hyphen-separated part that becomes the exact brand only after undoing a look-alike
            // substitution (paypa1-secure-login, amaz0n-support) is a deliberate imitation.
            boolean disguisedPart = brand.length() >= 5 && List.of(label.split("-")).stream()
                    .anyMatch(t -> !t.equals(brand) && deleet(t).equals(brand));
            if (disguisedPart || brand.length() >= 5 && (deleet.equals(brand) || (dist >= 1 && dist <= maxTypoDistance(brand) && Math.abs(label.length() - brand.length()) <= 2))) {
                out.add(ind("URL_TYPOSQUAT", prefix + "DECEPTIVE_DOMAIN", "Look-alike of a well-known domain",
                        "'" + registrable + "' closely resembles '" + brand + "'. Typosquatted domains imitate trusted sites.",
                        0.6, registrable));
                return;
            }
            if (u.pathAndQuery().toLowerCase(Locale.ROOT).matches(".*[/=._-]" + brand + "([/._-].*|$)")) {
                out.add(ind("URL_BRAND_IN_PATH", prefix + "DECEPTIVE_DOMAIN", "Brand name in path of unrelated domain",
                        "The link path mentions '" + brand + "' although the domain does not belong to it.", 0.35, registrable));
                return;
            }
        }
    }

    /** Undoes common look-alike substitutions: rn->m, vv->w and digits used as letters. */
    static String deleet(String s) {
        return s.replace("rn", "m").replace("vv", "w").replace('0', 'o').replace('1', 'l').replace('3', 'e').replace('5', 's').replace('4', 'a');
    }

    /** Short brand names tolerate one edit, longer ones two, to limit false positives. */
    static int maxTypoDistance(String brand) {
        return brand.length() >= 7 ? 2 : 1;
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    /** Extracts up to {@link #MAX_EMBEDDED_URLS} link-like strings from free text. */
    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return urls;
        }
        Matcher m = EMBEDDED_URL.matcher(text);
        while (m.find() && urls.size() < MAX_EMBEDDED_URLS) {
            String u = m.group(1).replaceAll("[.,;:!?)]+$", "");
            if (!urls.contains(u)) {
                urls.add(u);
            }
        }
        return urls;
    }

    private static Indicator ind(String code, String group, String title, String explanation, double weight, String evidence) {
        return new Indicator(code, group, title, explanation, weight, ID, ResultSource.LOCAL_HEURISTIC, evidence);
    }
}
