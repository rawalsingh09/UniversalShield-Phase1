package com.universalshield.detectors.common;

import com.universalshield.engine.Indicator;
import com.universalshield.engine.ResultSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared library of social-engineering text signals used by the message, call and
 * transaction detectors. Keeping them in one place means a tactic is described and weighted
 * consistently everywhere.
 */
public final class TextSignals {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern NEGATION = Pattern.compile("\\b(not|never|don'?t|do not|won'?t|no one|nobody)\\b[^.!?]{0,25}$", FLAGS);
    private static final int MAX_EVIDENCE = 60;

    public static final TextSignal URGENCY = signal("URGENCY_LANGUAGE", "URGENCY", "Urgency or time pressure",
            "Creating time pressure is a classic tactic to stop people from checking a request.", 0.25,
            "\\b(urgent(ly)?|immediately|act now|right away|within (24|48|12|2) ?(hours|hrs|minutes|mins)|expires? (today|soon|tonight)|last chance|final (notice|warning|reminder)|asap|without delay)\\b", false);
    public static final TextSignal OTP_REQUEST = signal("OTP_REQUEST", "OTP_REQUEST", "Request for an OTP or verification code",
            "Legitimate organisations never ask you to share a one-time password or verification code.", 0.7,
            "\\b(share|send|tell|forward|give|read out|provide|reply with|confirm)\\b[^.!?]{0,40}\\b(otp|one[- ]time (password|code|pin)|verification code|security code|\\d[- ]digit code|code (you|we) (sent|received))\\b", true);
    public static final TextSignal CREDENTIAL_REQUEST = signal("CREDENTIAL_REQUEST", "CREDENTIAL_REQUEST", "Request to verify account or credentials",
            "Asking you to 'verify' or 'update' account details through a message is how credential-harvesting starts.", 0.55,
            "\\b(verify|confirm|update|validate|re-?enter|reactivate|unlock)\\b[^.!?]{0,30}\\b(account|password|login|credentials|bank details|card details|kyc|pan|aadhaar|net ?banking|identity)\\b", true);
    public static final TextSignal SECRET_REQUEST = signal("PIN_CVV_REQUEST", "CREDENTIAL_REQUEST", "Request for PIN, CVV or card number",
            "No genuine bank, shop or authority asks for your PIN, CVV or full card number.", 0.7,
            "\\b(share|send|tell|enter|provide|give|confirm|read out)\\b[^.!?]{0,25}\\b(upi pin|atm pin|pin|cvv|cvc|card number|expiry date)\\b", true);
    public static final TextSignal KYC = signal("KYC_PRESSURE", "CREDENTIAL_REQUEST", "KYC update pressure",
            "Fake 'KYC expired/pending' notices are widely used to steal banking details.", 0.5,
            "\\bkyc\\b[^.!?]{0,30}\\b(update|expired?|pending|suspend|block|incomplete)", false);
    public static final TextSignal PRIZE = signal("PRIZE_LURE", "PRIZE_LURE", "Unexpected prize, reward or refund",
            "Unexpected winnings that require action are a common lure.", 0.45,
            "\\b(you('ve| have)? (won|been selected)|winner|lottery|jackpot|claim (your )?(prize|reward|gift|cashback)|free (iphone|gift|voucher|recharge)|lucky draw|congratulations[^.!?]{0,30}(won|selected|reward))", false);
    public static final TextSignal PAYMENT_REQUEST = signal("PAYMENT_DEMAND", "PAYMENT_REQUEST", "Payment or fee demand",
            "Being asked to pay a fee, duty or charge to receive something is typical of advance-fee fraud.", 0.4,
            "(\\b(pay|transfer|send|deposit)\\b[^.!?]{0,30}(\\bfee\\b|\\bcharges?\\b|\\bfine\\b|\\bduty\\b|\\brs\\.?\\s?\\d|\\binr\\b|₹|\\$\\s?\\d|\\busd\\b)|processing fee|registration fee|release (your|the) (funds|parcel|package|payment)|customs (duty|fee|charges)|advance fee|clearance fee|\\b(transfer|send|deposit|move)\\b[^.!?]{0,20}\\b(money|amount|funds|savings)\\b[^.!?]{0,30}\\b(verify|verification|safe account|secure account|clear your name|investigation))", false);
    public static final TextSignal IMPERSONATION = signal("ORG_IMPERSONATION", "IMPERSONATION", "Claims to be a trusted organisation",
            "Scammers often claim to be a bank, government agency, courier or big brand to gain trust.", 0.2,
            "\\b(bank|sbi|hdfc|icici|axis|kotak|paypal|amazon|apple|microsoft|netflix|income tax|irs|police|cyber ?(cell|crime)|customs|courier|fedex|dhl|india post|usps|rbi|reserve bank|government|trai|telecom department|electricity (board|department)|tax department|cbi|ed officer|narcotics)\\b", false);
    public static final TextSignal THREAT = signal("THREAT_LANGUAGE", "THREAT", "Threats or account-closure warnings",
            "Threats of arrest, fines or account closure are used to frighten people into acting.", 0.45,
            "\\b(account (will be |has been |is )?(blocked|suspended|closed|frozen|deactivated|on hold)|legal action|arrest(ed)?|warrant|digital arrest|penalty|(will be )?disconnect(ed|ion)|power (will be )?(cut|disconnected)|fine of|case (has been )?(filed|registered)|money laundering|illegal (parcel|items))", false);
    public static final TextSignal LINK_CTA = signal("LINK_CALL_TO_ACTION", "LINK_CTA", "Pushes you to click a link",
            "Messages that pressure you to click a link can lead to phishing pages or malware.", 0.25,
            "\\b(click|tap|open|visit|follow)\\b[^.!?]{0,20}\\b(link|here|below|url|http|www)", false);
    public static final TextSignal PAYMENT_METHOD = signal("UNUSUAL_PAYMENT_METHOD", "PAYMENT_METHOD", "Gift cards or crypto requested",
            "Gift cards and cryptocurrency are favoured by fraudsters because payments are hard to reverse.", 0.45,
            "\\b(gift ?cards?|itunes card|google play (card|code)|steam card|amazon (gift )?voucher|bitcoin|btc|usdt|crypto ?(currency)?|wallet address)\\b", false);
    public static final TextSignal SECRECY = signal("SECRECY_REQUEST", "SECRECY", "Asks for secrecy or isolation",
            "Telling you not to talk to anyone, or to stay on the line, isolates you from people who could warn you.", 0.4,
            "\\b((don'?t|do not|not|never|must not) (to )?(tell|inform|share with) (anyone|anybody|your family|family|the bank|police|(your )?(dad|mom|mum|father|mother|parents|husband|wife))(?![^.!?]{0,25}\\b(otp|pin|password|cvv|code|details))|keep (this|it) (secret|confidential|between us)|do not contact (anyone|the bank|police)|do not hang up|don'?t hang up|stay on (the )?(video )?(line|call)|do not disconnect)\\b", false);
    public static final TextSignal REMOTE_ACCESS = signal("REMOTE_ACCESS_REQUEST", "REMOTE_ACCESS", "Request to install remote-access or unknown apps",
            "Remote-access and screen-sharing apps let a scammer watch your screen, read OTPs and operate your accounts.", 0.65,
            "\\b(anydesk|any desk|teamviewer|team viewer|quick ?support|rustdesk|airdroid|screen ?shar(e|ing)|share (your )?screen|remote (access|control|desktop)|install (this|the|an?) (app|application|apk)|download (this|the) (app|apk)|\\.apk)\\b", false);
    public static final TextSignal FAKE_SUPPORT = signal("FAKE_SUPPORT", "FAKE_SUPPORT", "Unsolicited support, refund or charge claim",
            "Unexpected 'you were charged' or 'refund' contacts are a common entry point for tech-support and refund scams.", 0.35,
            "\\b(refund (is )?(pending|initiated|approved)|you (have been|were|will be) charged|subscription (renewal|will be renewed|auto-?renew)|cancel (your|the) (order|subscription)|tech(nical)? support|customer (care|support) (number|executive|team)|virus (found|detected)|(computer|device|phone) is (infected|hacked)|suspicious (activity|login|transaction) (on|in) your)", false);
    public static final TextSignal RECEIVE_MONEY = signal("PIN_TO_RECEIVE", "RECEIVE_MONEY_MYTH", "Asked to enter PIN or scan QR to receive money",
            "You never need to enter a PIN, scan a QR code or approve a request to RECEIVE money. Doing so sends money out.", 0.7,
            "((enter|type|use|put) (your |my |the )?(upi )?pin[^.!?]{0,40}(receive|get|credit|refund)|scan[^.!?]{0,20}qr[^.!?]{0,40}(receive|get|credit|refund)|(collect|payment) request[^.!?]{0,40}(receive|refund|credit)|approve[^.!?]{0,30}request[^.!?]{0,30}(receive|refund|credit))", false);
    public static final TextSignal TOO_GOOD = signal("TOO_GOOD_TO_BE_TRUE", "TOO_GOOD", "Too-good-to-be-true income or returns",
            "Promises of easy income, guaranteed returns or 'double your money' are hallmarks of job and investment scams.", 0.4,
            "\\b(work from home|part[- ]time job|earn (rs\\.?|₹|\\$|inr)?\\s?\\d[\\d,]*[^.!?]{0,15}(per|a|every) (day|hour|week)|guaranteed (returns?|profit)|double your (money|investment)|risk[- ]free (investment|returns?)|task[- ]based (job|earning)|like (videos|posts) and earn)\\b", false);

    public static final List<TextSignal> ALL = List.of(URGENCY, OTP_REQUEST, CREDENTIAL_REQUEST, SECRET_REQUEST, KYC,
            PRIZE, PAYMENT_REQUEST, IMPERSONATION, THREAT, LINK_CTA, PAYMENT_METHOD, SECRECY, REMOTE_ACCESS,
            FAKE_SUPPORT, RECEIVE_MONEY, TOO_GOOD);

    private TextSignals() {
    }

    private static TextSignal signal(String code, String group, String title, String explanation, double weight,
                                     String regex, boolean negatable) {
        return new TextSignal(code, group, title, explanation, weight, Pattern.compile(regex, FLAGS), negatable);
    }

    /**
     * Matches the given signals against text. Each signal yields at most one indicator.
     *
     * @param overrides optional per-signal weight overrides (by code) for the calling detector
     */
    public static List<Indicator> match(String text, List<TextSignal> signals, Map<String, Double> overrides,
                                        String detectorId, String groupPrefix) {
        List<Indicator> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (TextSignal s : signals) {
            String evidence = firstMatch(text, s);
            if (evidence != null) {
                double w = overrides.getOrDefault(s.code(), s.defaultWeight());
                out.add(new Indicator(s.code(), groupPrefix + s.group(), s.title(), s.explanation(), w,
                        detectorId, ResultSource.LOCAL_HEURISTIC, evidence));
            }
        }
        return out;
    }

    private static String firstMatch(String text, TextSignal s) {
        Matcher m = s.pattern().matcher(text);
        while (m.find()) {
            if (s.negatable()) {
                String before = text.substring(Math.max(0, m.start() - 30), m.start());
                if (NEGATION.matcher(before).find()) {
                    continue;
                }
            }
            return evidence(m.group());
        }
        return null;
    }

    /** Shortens a match and masks long digit runs so evidence never contains codes or numbers. */
    public static String evidence(String raw) {
        String masked = raw.replaceAll("\\d{4,}", "••••").replaceAll("\\s+", " ").trim();
        return masked.length() > MAX_EVIDENCE ? masked.substring(0, MAX_EVIDENCE - 1) + "…" : masked;
    }

    /** Counts words, used by detectors to judge whether input is long enough to assess. */
    public static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
