package com.universalshield.service;

import com.universalshield.api.IntegrationTestBase;
import com.universalshield.dto.ScanRequest;
import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.input.MessageInput;
import com.universalshield.repository.ScanRepository;
import com.universalshield.user.User;
import com.universalshield.user.UserPreference;
import com.universalshield.user.UserPreferenceRepository;
import com.universalshield.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionServiceIntegrationTest extends IntegrationTestBase {

    @Autowired RetentionService retention;
    @Autowired ScanService scanService;
    @Autowired ScanRepository scans;
    @Autowired UserRepository users;
    @Autowired UserPreferenceRepository prefs;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void scansOlderThanUserRetentionArePurged() {
        User u = users.save(new User(uniqueEmail(), "Retention", "{noop}x"));
        prefs.save(new UserPreference(u.getId(), 7));
        ScanRequest req = new ScanRequest(ScanCategory.MESSAGE, new MessageInput("synthetic retention message text", "SMS", null), null, null, null, null);
        var old = scanService.createEntity(u.getId(), req, false);
        var fresh = scanService.createEntity(u.getId(), req, false);
        // created_at is not updatable through JPA (by design), so back-date the row directly
        jdbc.update("UPDATE scans SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minus(Duration.ofDays(8))), old.getId());
        int deleted = retention.purgeExpired();
        assertThat(deleted).isGreaterThanOrEqualTo(1);
        assertThat(scans.findById(old.getId())).isEmpty();
        assertThat(scans.findById(fresh.getId())).isPresent();
    }
}
