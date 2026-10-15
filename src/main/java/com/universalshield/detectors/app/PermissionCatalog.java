package com.universalshield.detectors.app;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Built-in knowledge base of sensitive and normal Android permissions. */
public final class PermissionCatalog {

    /** Permissions considered low-risk; they are recognised but produce no indicator. */
    static final Set<String> NORMAL = Set.of("INTERNET", "ACCESS_NETWORK_STATE", "ACCESS_WIFI_STATE", "VIBRATE",
            "WAKE_LOCK", "FOREGROUND_SERVICE", "POST_NOTIFICATIONS", "RECEIVE_BOOT_COMPLETED", "BLUETOOTH",
            "BLUETOOTH_CONNECT", "NFC", "USE_BIOMETRIC", "USE_FINGERPRINT", "FLASHLIGHT", "SET_WALLPAPER",
            "CHANGE_WIFI_STATE", "BILLING", "SCHEDULE_EXACT_ALARM");

    private static final Map<String, PermissionRule> RULES = new HashMap<>();

    static {
        Set<String> messaging = Set.of("MESSAGING");
        rule("READ_SMS", "SMS_ACCESS", 0.55, "Read SMS messages", "Can read every SMS, including bank OTPs.", messaging);
        rule("RECEIVE_SMS", "SMS_ACCESS", 0.55, "Receive SMS messages", "Can intercept incoming SMS, including OTPs.", messaging);
        rule("SEND_SMS", "SMS_ACCESS", 0.45, "Send SMS messages", "Can send SMS (including to premium numbers) without you noticing.", messaging);
        rule("RECEIVE_MMS", "SMS_ACCESS", 0.3, "Receive MMS", "Can intercept multimedia messages.", messaging);
        rule("RECEIVE_WAP_PUSH", "SMS_ACCESS", 0.3, "Receive WAP push", "Can intercept carrier push messages.", messaging);
        rule("BIND_ACCESSIBILITY_SERVICE", "ACCESSIBILITY", 0.6, "Accessibility service",
                "Can read everything on screen and perform taps for you; heavily abused by banking trojans.", Set.of("ACCESSIBILITY"));
        rule("SYSTEM_ALERT_WINDOW", "OVERLAY", 0.4, "Display over other apps",
                "Can draw fake login screens over real banking apps.", Set.of("ACCESSIBILITY", "MESSAGING"));
        rule("BIND_NOTIFICATION_LISTENER_SERVICE", "NOTIFICATIONS", 0.45, "Read all notifications",
                "Can read OTPs and private messages shown in notifications.", Set.of("ACCESSIBILITY", "UTILITY", "HEALTH"));
        rule("BIND_DEVICE_ADMIN", "DEVICE_ADMIN", 0.6, "Device administrator",
                "Can lock the device, wipe data and resist being uninstalled.", Set.of("SECURITY"));
        rule("REQUEST_INSTALL_PACKAGES", "INSTALLS", 0.4, "Install other apps",
                "Can download and install further apps, a common malware 'dropper' technique.", Set.of("UTILITY", "SECURITY"));
        rule("READ_CONTACTS", "CONTACTS", 0.25, "Read contacts",
                "Exposes everyone in your address book; predatory loan apps use this to harass contacts.", Set.of("MESSAGING", "SOCIAL"));
        rule("WRITE_CONTACTS", "CONTACTS", 0.2, "Modify contacts", "Can change or add contacts.", Set.of("MESSAGING", "SOCIAL"));
        rule("READ_CALL_LOG", "CALL_LOG", 0.4, "Read call history", "Exposes who you call and when.", Set.of("MESSAGING", "SECURITY"));
        rule("WRITE_CALL_LOG", "CALL_LOG", 0.3, "Modify call history", "Can hide or alter call records.", Set.of("SECURITY"));
        rule("PROCESS_OUTGOING_CALLS", "CALL_LOG", 0.4, "Redirect outgoing calls", "Can see or redirect numbers you dial.", Set.of("SECURITY"));
        rule("CALL_PHONE", "CALLS", 0.2, "Make phone calls", "Can place calls, possibly to premium numbers.", Set.of("MESSAGING", "SECURITY"));
        rule("ANSWER_PHONE_CALLS", "CALLS", 0.25, "Answer calls", "Can answer incoming calls automatically.", Set.of("SECURITY", "ACCESSIBILITY"));
        rule("ACCESS_FINE_LOCATION", "LOCATION", 0.2, "Precise location", "Reveals exactly where you are.",
                Set.of("NAVIGATION", "SHOPPING", "HEALTH", "SOCIAL", "CAMERA_PHOTO"));
        rule("ACCESS_COARSE_LOCATION", "LOCATION", 0.1, "Approximate location", "Reveals your approximate area.",
                Set.of("NAVIGATION", "SHOPPING", "HEALTH", "SOCIAL", "CAMERA_PHOTO", "BANKING_FINANCE"));
        rule("ACCESS_BACKGROUND_LOCATION", "LOCATION", 0.35, "Location in background",
                "Tracks your location even when the app is closed.", Set.of("NAVIGATION", "HEALTH"));
        rule("RECORD_AUDIO", "MICROPHONE", 0.3, "Microphone", "Can record audio around you.",
                Set.of("MESSAGING", "SOCIAL", "CAMERA_PHOTO", "ACCESSIBILITY"));
        rule("CAMERA", "CAMERA", 0.15, "Camera", "Can take photos and video.",
                Set.of("CAMERA_PHOTO", "MESSAGING", "SOCIAL", "BANKING_FINANCE", "SHOPPING", "LOAN"));
        rule("READ_PHONE_STATE", "DEVICE_ID", 0.2, "Phone state and identity", "Exposes phone number, network and call state.",
                Set.of("MESSAGING", "BANKING_FINANCE", "SECURITY"));
        rule("READ_PHONE_NUMBERS", "DEVICE_ID", 0.2, "Read phone numbers", "Exposes your phone number.",
                Set.of("MESSAGING", "BANKING_FINANCE", "SECURITY"));
        rule("QUERY_ALL_PACKAGES", "APP_INVENTORY", 0.2, "See all installed apps",
                "Reveals which banking and other apps you use.", Set.of("SECURITY", "UTILITY"));
        rule("MANAGE_EXTERNAL_STORAGE", "STORAGE", 0.3, "All files access", "Can read and change all files on shared storage.",
                Set.of("UTILITY", "SECURITY"));
        rule("READ_EXTERNAL_STORAGE", "STORAGE", 0.1, "Read storage", "Can read photos and files on shared storage.",
                Set.of("CAMERA_PHOTO", "MESSAGING", "SOCIAL", "UTILITY"));
        rule("READ_MEDIA_IMAGES", "STORAGE", 0.1, "Read photos", "Can read your photos.", Set.of("CAMERA_PHOTO", "MESSAGING", "SOCIAL", "UTILITY"));
        rule("GET_ACCOUNTS", "ACCOUNTS", 0.2, "List accounts on device", "Reveals which accounts are signed in on the device.",
                Set.of("MESSAGING", "SOCIAL", "UTILITY"));
        rule("BODY_SENSORS", "SENSORS", 0.15, "Body sensors", "Reads health sensor data.", Set.of("HEALTH"));
    }

    private PermissionCatalog() {
    }

    private static void rule(String name, String group, double weight, String title, String explanation, Set<String> justified) {
        RULES.put(name, new PermissionRule(group, weight, title, explanation, justified));
    }

    /** Strips the "android.permission." prefix and upper-cases the name. */
    public static String normalize(String permission) {
        String p = permission.trim();
        int dot = p.lastIndexOf('.');
        if (dot >= 0) {
            p = p.substring(dot + 1);
        }
        return p.toUpperCase(Locale.ROOT);
    }

    public static Optional<PermissionRule> rule(String normalized) {
        return Optional.ofNullable(RULES.get(normalized));
    }

    public static boolean isNormal(String normalized) {
        return NORMAL.contains(normalized);
    }
}
