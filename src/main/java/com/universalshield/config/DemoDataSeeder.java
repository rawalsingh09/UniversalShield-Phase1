package com.universalshield.config;

import com.universalshield.dto.RegisterRequest;
import com.universalshield.dto.ScanRequest;
import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.input.AppPermissionInput;
import com.universalshield.engine.input.CallInput;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.engine.input.TransactionInput;
import com.universalshield.engine.input.UrlInput;
import com.universalshield.auth.AuthService;
import com.universalshield.service.ScanService;
import com.universalshield.user.User;
import com.universalshield.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Creates a clearly labeled demonstration account with scans produced by running the real engine
 * on SYNTHETIC examples. Enabled only when universalshield.demo.seed=true and a demo password is
 * supplied (the dev profile does this). Every seeded scan has demo = true and is labeled DEMO in the UI.
 */
@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppProperties props;
    private final UserRepository users;
    private final AuthService auth;
    private final ScanService scans;

    public DemoDataSeeder(AppProperties props, UserRepository users, AuthService auth, ScanService scans) {
        this.props = props;
        this.users = users;
        this.auth = auth;
        this.scans = scans;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        var demo = props.demo();
        if (!demo.seed() || demo.password() == null || demo.password().isBlank()) {
            return;
        }
        if (users.existsByEmailIgnoreCase(demo.email())) {
            return;
        }
        User u = auth.register(new RegisterRequest(demo.email(), "Demo Student", demo.password()), null);
        for (ScanRequest r : samples()) {
            scans.createEntity(u.getId(), r, true);
        }
        log.info("Created demonstration account {} with {} synthetic DEMO scans", demo.email(), samples().size());
    }

    static List<ScanRequest> samples() {
        return List.of(
                new ScanRequest(ScanCategory.MESSAGE, new MessageInput(
                        "URGENT: Your SBI account will be blocked today. Update your KYC immediately at http://sbi-kyc-update.xyz/login and share the OTP to verify.",
                        "SMS", "VK-SBIINF"), null, null, null, null),
                new ScanRequest(ScanCategory.MESSAGE, new MessageInput(
                        "Hi, are we still meeting at the library at 4pm to finish the database assignment?", "CHAT", "Classmate"),
                        null, null, null, null),
                new ScanRequest(ScanCategory.URL, null, new UrlInput("http://paypal.com.account-verify.top/signin?session=8812"),
                        null, null, null),
                new ScanRequest(ScanCategory.URL, null, new UrlInput("https://www.wikipedia.org/"), null, null, null),
                new ScanRequest(ScanCategory.TRANSACTION, null, null, new TransactionInput(new BigDecimal("15000"), "INR",
                        "unknown-seller@upi", true, "UPI", 3, true,
                        "Buyer on marketplace says enter your UPI PIN to receive the payment for my old phone"), null, null),
                new ScanRequest(ScanCategory.CALL, null, null, null, new CallInput("Cyber crime police officer", "+91 90000 00000",
                        "Caller said a parcel in my name had illegal items, I am under digital arrest, must stay on the call, install AnyDesk and transfer money to a safe account.",
                        25), null),
                new ScanRequest(ScanCategory.APP_PERMISSION, null, null, null, null, new AppPermissionInput("Super Bright Torch",
                        "FLASHLIGHT", "UNKNOWN_SOURCE", List.of("android.permission.CAMERA", "android.permission.READ_SMS",
                        "android.permission.RECEIVE_SMS", "android.permission.BIND_ACCESSIBILITY_SERVICE",
                        "android.permission.READ_CONTACTS", "android.permission.SYSTEM_ALERT_WINDOW"))));
    }
}
