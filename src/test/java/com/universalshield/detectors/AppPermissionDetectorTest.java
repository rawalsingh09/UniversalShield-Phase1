package com.universalshield.detectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalshield.detectors.app.AppPermissionDetector;
import com.universalshield.detectors.app.PermissionCatalog;
import com.universalshield.detectors.app.PermissionReportParser;
import com.universalshield.engine.FraudEvent;
import com.universalshield.engine.input.AppPermissionInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.universalshield.detectors.DetectorTestSupport.codes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppPermissionDetectorTest {

    private final AppPermissionDetector detector = new AppPermissionDetector();

    private com.universalshield.engine.DetectorResult scan(String category, String source, String... perms) {
        return detector.analyze(FraudEvent.ofApp(new AppPermissionInput("Test App", category, source, List.of(perms)), null));
    }

    @Test
    void flashlightWithSmsAndAccessibilityIsBankingTrojanPattern() {
        var r = scan("FLASHLIGHT", "UNKNOWN_SOURCE", "android.permission.READ_SMS", "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "CAMERA", "READ_CONTACTS", "SYSTEM_ALERT_WINDOW");
        assertThat(codes(r)).contains("PERM_COMBO_BANKING_TROJAN", "APP_SIDELOADED", "PERM_EXCESSIVE", "PERM_READ_SMS");
        assertThat(r.notes()).anyMatch(n -> n.contains("cannot prove that an app is malware"));
    }

    @Test
    void justifiedPermissionsGetReducedWeight() {
        var messaging = scan("MESSAGING", "PLAY_STORE", "READ_SMS");
        var game = scan("GAME", "PLAY_STORE", "READ_SMS");
        double justified = messaging.indicators().get(0).weight();
        double unjustified = game.indicators().get(0).weight();
        assertThat(justified).isLessThan(unjustified);
        assertThat(messaging.indicators().get(0).title()).contains("appears justified");
        assertThat(codes(messaging)).doesNotContain("PERM_COMBO_BANKING_TROJAN");
    }

    @Test
    void loanAppReadingContactsIsFlagged() {
        assertThat(codes(scan("LOAN", "PLAY_STORE", "READ_CONTACTS", "CAMERA"))).contains("PERM_COMBO_PREDATORY_LOAN");
    }

    @Test
    void normalPermissionsProduceNoIndicators() {
        var r = scan("GAME", "PLAY_STORE", "INTERNET", "VIBRATE", "ACCESS_NETWORK_STATE");
        assertThat(r.indicators()).isEmpty();
    }

    @Test
    void unknownPermissionsAreListedNotScored() {
        var r = scan("OTHER", "PLAY_STORE", "com.vendor.CUSTOM_THING");
        assertThat(r.indicators()).isEmpty();
        assertThat(r.inputSufficient()).isFalse();
        assertThat(r.notes()).anyMatch(n -> n.contains("CUSTOM_THING"));
    }

    @Test
    void duplicatePermissionsCountOnce() {
        var r = scan("GAME", "PLAY_STORE", "READ_SMS", "android.permission.READ_SMS", "read_sms");
        assertThat(codes(r)).containsOnlyOnce("PERM_READ_SMS");
    }

    @Test
    void normalizeStripsPrefix() {
        assertThat(PermissionCatalog.normalize("android.permission.read_sms")).isEqualTo("READ_SMS");
    }

    @Test
    void parserHandlesAaptTextJsonAndPlainLists() {
        var parser = new PermissionReportParser(new ObjectMapper());
        assertThat(parser.parse("uses-permission: name='android.permission.READ_SMS'\nuses-permission: name='android.permission.CAMERA'").permissions())
                .containsExactly("READ_SMS", "CAMERA");
        var json = parser.parse("{\"appName\":\"X\",\"permissions\":[\"android.permission.READ_CONTACTS\",\"<script>\"]}");
        assertThat(json.appName()).isEqualTo("X");
        assertThat(json.permissions()).containsExactly("READ_CONTACTS");
        assertThat(parser.parse("READ_SMS, camera\nINTERNET").permissions()).containsExactly("READ_SMS", "CAMERA", "INTERNET");
        assertThatThrownBy(() -> parser.parse("{not json")).isInstanceOf(IllegalArgumentException.class);
    }
}
