package com.universalshield.service;

import com.universalshield.repository.AuditEventRepository;
import com.universalshield.repository.ScanRepository;
import com.universalshield.user.UserPreference;
import com.universalshield.user.UserPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Enforces each user's chosen retention period by deleting older scans, and removes audit
 * events older than a year. Runs daily (universalshield.retention.cron).
 */
@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);
    static final Duration AUDIT_RETENTION = Duration.ofDays(365);

    private final ScanRepository scans;
    private final UserPreferenceRepository preferences;
    private final AuditEventRepository audit;
    private final Clock clock;

    public RetentionService(ScanRepository scans, UserPreferenceRepository preferences, AuditEventRepository audit, Clock clock) {
        this.scans = scans;
        this.preferences = preferences;
        this.audit = audit;
        this.clock = clock;
    }

    @Scheduled(cron = "${universalshield.retention.cron:0 30 3 * * *}")
    @Transactional
    public int purgeExpired() {
        Instant now = Instant.now(clock);
        int deleted = 0;
        for (UserPreference p : preferences.findAll()) {
            List<Long> ids = scans.findIdsOlderThan(p.getUserId(), now.minus(Duration.ofDays(p.getRetentionDays())));
            if (!ids.isEmpty()) {
                scans.deleteAllByIdInBatch(ids);
                deleted += ids.size();
            }
        }
        int auditDeleted = audit.deleteOlderThan(now.minus(AUDIT_RETENTION));
        log.info("Retention purge removed {} scans and {} audit events", deleted, auditDeleted);
        return deleted;
    }
}
