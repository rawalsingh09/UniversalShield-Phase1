package com.universalshield.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps indicator groups and risk levels to concrete next steps.
 * Group keys may be prefixed by a namespace ("url:", "ml:"); the suffix is used for lookup.
 */
public final class RecommendationCatalog {

    private static final int MAX_RECOMMENDATIONS = 6;

    private final Map<String, Recommendation> byGroup = new LinkedHashMap<>();

    public RecommendationCatalog() {
        put("OTP_REQUEST", "Never share a one-time password (OTP) or verification code with anyone.",
                "Banks and genuine services never ask you to read out or forward an OTP.", 1);
        put("CREDENTIAL_REQUEST", "Do not enter your password through a link you received. Open the official app or type the address yourself.",
                "Credential-harvesting pages copy the look of real login pages.", 1);
        put("REMOTE_ACCESS", "Do not install screen-sharing or remote-access apps at a caller's request. Hang up.",
                "Remote access lets a scammer see your OTPs and control your banking apps.", 1);
        put("RECEIVE_MONEY_MYTH", "You never need to enter a PIN or approve a request to RECEIVE money. Decline the request.",
                "Entering your PIN on a 'collect' request sends money out of your account.", 1);
        put("PAYMENT_REQUEST", "Do not pay until you have verified the request through a contact method you already trust.",
                "Scammers create a reason to pay quickly before you can check.", 2);
        put("PAYMENT_METHOD", "Be very cautious with gift cards, crypto or wire transfers requested by someone else.",
                "These methods are hard or impossible to reverse.", 2);
        put("IMPERSONATION", "Contact the organisation directly using the number on its official website or your card.",
                "The sender or caller may be impersonating a trusted organisation.", 2);
        put("THREAT", "Do not act on threats of arrest, account closure or fines delivered by message or call.",
                "Genuine authorities do not demand immediate payment by phone or chat.", 2);
        put("URGENCY", "Take your time. Pressure to act immediately is a common manipulation tactic.",
                "Urgency is designed to stop you from checking.", 3);
        put("PRIZE_LURE", "Ignore unexpected prizes or refunds that require a fee or personal details.",
                "Real prizes do not require you to pay to receive them.", 3);
        put("SECRECY", "Talk to someone you trust before acting. Requests for secrecy are a warning sign.",
                "Scammers isolate victims so nobody can warn them.", 2);
        put("DECEPTIVE_DOMAIN", "Do not open this link. Type the official website address yourself.",
                "The domain appears designed to look like a different, trusted site.", 1);
        put("OBFUSCATION", "Do not open links that hide their real destination.",
                "Obfuscated URLs are used to disguise malicious destinations.", 1);
        put("DOWNLOAD", "Do not download or install files from this link.",
                "The link points to an installable or executable file.", 1);
        put("SHORTENER", "Expand shortened links with a trusted preview service before opening, or avoid them.",
                "Shorteners hide the final destination.", 3);
        put("HOST_FORM", "Be cautious with links that use raw IP addresses or unusual ports.",
                "Legitimate services almost always use a registered domain name.", 2);
        put("TRANSPORT", "Never enter personal or payment data on a page without HTTPS. HTTPS alone also does not prove a site is safe.",
                "Without HTTPS, data is sent unencrypted.", 3);
        put("AMOUNT_ANOMALY", "Double-check the amount. It is unusual compared with your history.",
                "Unusually large payments are a common feature of fraud.", 2);
        put("RECIPIENT", "Confirm the new recipient's identity through a separate, trusted channel.",
                "First-time payees are where most authorised payment fraud happens.", 2);
        put("VELOCITY", "Stop retrying the payment. Repeated attempts may indicate pressure or a scam.",
                "Fraudsters often push victims to retry after failures.", 2);
        put("KNOWN_RISKY", "You previously received a high-risk result for this recipient/number/domain. Do not proceed without independent verification.",
                "Your own history flagged this subject before.", 1);
        put("SMS_ACCESS", "Deny SMS permissions unless this app is your messaging app.",
                "SMS access can be used to read OTPs.", 1);
        put("ACCESSIBILITY", "Do not enable accessibility access for this app unless you rely on it as an assistive tool.",
                "Accessibility services can read the screen and tap on your behalf.", 1);
        put("OVERLAY", "Deny 'display over other apps' unless clearly needed.",
                "Overlays can draw fake login screens over real apps.", 2);
        put("DEVICE_ADMIN", "Do not grant device administrator rights to this app.",
                "Device admin rights can prevent uninstalling the app.", 1);
        put("COMBINATION", "Uninstall or do not install this app until you can verify it with a trusted source.",
                "This combination of permissions matches known banking-trojan behaviour.", 1);
        put("INSTALL_SOURCE", "Install apps only from official stores or the developer's verified website.",
                "Sideloaded apps skip store security review.", 2);
        put("EXCESSIVE", "Review whether this app needs all of these permissions; deny what it does not need.",
                "Requesting far more access than its purpose requires is a warning sign.", 3);
        put("NOTIFICATIONS", "Deny notification access unless the app clearly needs it.",
                "Notification access lets an app read OTPs shown in notifications.", 2);
        put("PRIVACY", "Grant contacts, location, microphone or call-log access only when the feature needs it.",
                "These permissions expose personal data.", 3);
        put("LINK_CTA", "Do not click links in unexpected messages. Go to the official site or app yourself.",
                "Links in messages are the most common route to phishing pages.", 2);
        put("FAKE_SUPPORT", "Ignore unsolicited 'refund' or 'you were charged' contacts. Check your account in the official app.",
                "Fake-support scams start by inventing a problem for you.", 2);
        put("TOO_GOOD", "Be sceptical of easy money, guaranteed returns or paid 'tasks'.",
                "Job and investment scams promise income, then demand deposits.", 2);
        put("LURE_KEYWORDS", "Do not trust a link because it contains words like 'secure' or 'verify'.",
                "Anyone can put those words into a web address.", 3);
        put("TLD", "Check the full domain carefully before trusting it.",
                "Some domain endings are frequently used for short-lived abuse sites.", 4);
        put("SUBDOMAIN", "Read the domain from right to left: the part just before the ending is the real owner.",
                "Long subdomain chains push the real domain out of view.", 3);
        put("STRUCTURE", "Check the full address before opening it.", "The link has an unusual structure.", 4);
        put("REPUTATION", "Do not open this link or engage with this number.",
                "A verified reputation provider has listed it.", 0);
        put("INSTALLS", "Do not allow this app to install other apps.",
                "Installing further apps is a common way malware spreads.", 2);
        put("NUMBER_FORMAT", "Be careful with calls from unusual or hidden numbers.", "Numbers can be spoofed or masked.", 4);
        for (String g : new String[] {"CONTACTS", "CALL_LOG", "LOCATION", "MICROPHONE", "CAMERA", "DEVICE_ID",
                "APP_INVENTORY", "STORAGE", "ACCOUNTS", "CALLS", "SENSORS"}) {
            byGroup.put(g, byGroup.get("PRIVACY"));
        }
        put("MODEL", "Treat the model signal as supporting evidence only and review the matched rules.",
                "Statistical models make mistakes, especially on unfamiliar text.", 4);
    }

    private void put(String group, String action, String reason, int priority) {
        byGroup.put(group, new Recommendation(action, reason, priority));
    }

    public List<Recommendation> recommend(List<Indicator> indicators, RiskLevel level) {
        Map<String, Recommendation> chosen = new LinkedHashMap<>();
        chosen.put("LEVEL", forLevel(level));
        indicators.stream()
                .sorted(Comparator.comparingDouble(Indicator::weight).reversed())
                .forEach(i -> {
                    String key = suffix(i.group());
                    Recommendation r = byGroup.get(key);
                    if (r != null) {
                        chosen.putIfAbsent(key, r);
                    }
                });
        List<Recommendation> list = new ArrayList<>(new java.util.LinkedHashSet<>(chosen.values()));
        list.sort(Comparator.comparingInt(Recommendation::priority));
        return list.size() > MAX_RECOMMENDATIONS ? list.subList(0, MAX_RECOMMENDATIONS) : list;
    }

    static Recommendation forLevel(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> new Recommendation("Stop. Do not click, pay, install or share anything related to this.",
                    "Several strong warning signs were found together.", 0);
            case HIGH -> new Recommendation("Pause and verify independently before taking any action.",
                    "Strong warning signs were found. This is not proof of fraud, but the risk is significant.", 0);
            case MEDIUM -> new Recommendation("Verify the request through an official channel you already trust.",
                    "Some warning signs were found.", 1);
            case LOW -> new Recommendation("No strong warning signs were found, but stay alert to unexpected requests.",
                    "A low score does not guarantee that something is safe.", 5);
        };
    }

    static String suffix(String group) {
        int idx = group.indexOf(':');
        return idx >= 0 ? group.substring(idx + 1) : group;
    }
}
