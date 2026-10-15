package com.universalshield.detectors.app;

import com.universalshield.engine.DetectionModule;
import com.universalshield.engine.DetectorResult;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.Indicator;
import com.universalshield.engine.ResultSource;
import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.input.AppPermissionInput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Flags potentially excessive app permissions and explains whether each appears justified by
 * the app's stated purpose. Permission analysis can never prove that an app is malware.
 */
@Component
public class AppPermissionDetector implements DetectionModule {

    public static final String ID = "app-permission-heuristic";
    static final String VERSION = "1.0.0";
    /** Justified permissions still count a little, so the user sees them, but far less. */
    static final double JUSTIFIED_FACTOR = 0.25;
    static final double MIN_WEIGHT = 0.05;
    private static final Set<String> MINIMAL_PURPOSE = Set.of("FLASHLIGHT", "GAME", "UTILITY");

    @Override public String id() { return ID; }
    @Override public String displayName() { return "App permission risk analyzer"; }
    @Override public String version() { return VERSION; }
    @Override public String description() {
        return "Flags sensitive permissions (SMS, accessibility, overlays, notifications, device admin, contacts, location) "
                + "and judges whether they fit the app's stated purpose. Cannot prove an app is malware.";
    }
    @Override public Set<ScanCategory> categories() { return Set.of(ScanCategory.APP_PERMISSION); }
    @Override public ResultSource sourceType() { return ResultSource.LOCAL_HEURISTIC; }

    @Override
    public boolean appliesTo(FraudEvent event) {
        return event.category() == ScanCategory.APP_PERMISSION && event.app() != null;
    }

    @Override
    public DetectorResult analyze(FraudEvent event) {
        AppPermissionInput app = event.app();
        String category = app.appCategory();
        List<Indicator> out = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Set<String> unjustifiedGroups = new LinkedHashSet<>();
        Set<String> unknown = new LinkedHashSet<>();
        Set<String> seen = new LinkedHashSet<>();
        int unjustifiedSensitive = 0;

        for (String raw : app.permissions()) {
            String p = PermissionCatalog.normalize(raw);
            if (!seen.add(p) || PermissionCatalog.isNormal(p)) {
                continue;
            }
            var rule = PermissionCatalog.rule(p);
            if (rule.isEmpty()) {
                unknown.add(p);
                continue;
            }
            PermissionRule r = rule.get();
            boolean justified = r.justifiedFor().contains(category);
            double weight = justified ? Math.max(MIN_WEIGHT, r.weight() * JUSTIFIED_FACTOR) : r.weight();
            String title = r.title() + (justified ? " (appears justified for this type of app)" : " (not obviously needed)");
            out.add(new Indicator("PERM_" + p, r.group(), title, r.explanation(), weight, ID,
                    ResultSource.LOCAL_HEURISTIC, p));
            if (!justified) {
                unjustifiedGroups.add(r.group());
                unjustifiedSensitive++;
            }
        }

        boolean smsUnjustified = unjustifiedGroups.contains("SMS_ACCESS");
        boolean screenControl = unjustifiedGroups.contains("ACCESSIBILITY") || unjustifiedGroups.contains("OVERLAY")
                || unjustifiedGroups.contains("NOTIFICATIONS");
        if (smsUnjustified && screenControl) {
            out.add(new Indicator("PERM_COMBO_BANKING_TROJAN", "COMBINATION", "Banking-trojan permission pattern",
                    "SMS access combined with screen-reading or overlay access matches how banking trojans steal OTPs and logins.",
                    0.75, ID, ResultSource.LOCAL_HEURISTIC, "SMS + accessibility/overlay/notifications"));
        } else if ("LOAN".equals(category) && (seen.contains("READ_CONTACTS") || seen.contains("READ_SMS")
                || seen.contains("READ_CALL_LOG"))) {
            out.add(new Indicator("PERM_COMBO_PREDATORY_LOAN", "COMBINATION", "Predatory loan-app pattern",
                    "Loan apps that read contacts, SMS or call logs have been used to harass borrowers and their contacts.",
                    0.55, ID, ResultSource.LOCAL_HEURISTIC, "LOAN + contacts/SMS/call log"));
        }
        if (unjustifiedSensitive >= 5) {
            out.add(new Indicator("PERM_EXCESSIVE", "EXCESSIVE", "Excessive sensitive permissions",
                    unjustifiedSensitive + " sensitive permissions do not obviously fit a " + category.toLowerCase() + " app.",
                    0.25, ID, ResultSource.LOCAL_HEURISTIC, unjustifiedSensitive + " permissions"));
        } else if (MINIMAL_PURPOSE.contains(category) && unjustifiedSensitive >= 3) {
            out.add(new Indicator("PERM_EXCESSIVE_SIMPLE_APP", "EXCESSIVE", "Simple app asking for a lot",
                    "A " + category.toLowerCase() + " app rarely needs " + unjustifiedSensitive + " sensitive permissions.",
                    0.35, ID, ResultSource.LOCAL_HEURISTIC, unjustifiedSensitive + " permissions"));
        }
        if ("UNKNOWN_SOURCE".equals(app.installSource())) {
            out.add(new Indicator("APP_SIDELOADED", "INSTALL_SOURCE", "Installed from an unknown source",
                    "Apps from links, APK files or third-party sites skip app-store security review.",
                    0.3, ID, ResultSource.LOCAL_HEURISTIC, "UNKNOWN_SOURCE"));
        }
        if (!unknown.isEmpty()) {
            notes.add(unknown.size() + " permission(s) are not in the built-in catalog and were not scored: "
                    + String.join(", ", unknown.stream().limit(10).toList()) + (unknown.size() > 10 ? ", …" : ""));
        }
        notes.add("Permission analysis cannot prove that an app is malware, nor that it is safe. "
                + "'Justified' is judged only from the app category you selected.");
        boolean sufficient = seen.size() > unknown.size();
        return new DetectorResult(ID, VERSION, ResultSource.LOCAL_HEURISTIC, out, notes, sufficient);
    }
}
