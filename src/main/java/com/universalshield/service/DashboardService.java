package com.universalshield.service;

import com.universalshield.dto.DashboardSummary;
import com.universalshield.engine.RiskLevel;
import com.universalshield.engine.ScanCategory;
import com.universalshield.repository.ScanRepository;
import com.universalshield.user.UserPreference;
import com.universalshield.user.UserPreferenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds dashboard statistics from the user's own stored scans. No hardcoded or global numbers. */
@Service
public class DashboardService {

    static final String NOTE = "These statistics describe only the scans you submitted and still have stored. "
            + "They are not detection-accuracy figures and say nothing about how common fraud is in general. "
            + "Scans marked DEMO were generated from synthetic examples.";

    private final ScanRepository scans;
    private final UserPreferenceRepository preferences;
    private final ScanMapper mapper;

    public DashboardService(ScanRepository scans, UserPreferenceRepository preferences, ScanMapper mapper) {
        this.scans = scans;
        this.preferences = preferences;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(Long userId) {
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (ScanCategory c : ScanCategory.values()) {
            byCategory.put(c.name(), 0L);
        }
        for (Object[] row : scans.countByCategory(userId)) {
            byCategory.put(((ScanCategory) row[0]).name(), (Long) row[1]);
        }
        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (RiskLevel l : RiskLevel.values()) {
            byLevel.put(l.name(), 0L);
        }
        for (Object[] row : scans.countByLevel(userId)) {
            byLevel.put(((RiskLevel) row[0]).name(), (Long) row[1]);
        }
        var recent = scans.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5)).stream().map(mapper::toSummary).toList();
        var high = scans.findByUserIdAndRiskLevelInOrderByCreatedAtDesc(userId, List.of(RiskLevel.HIGH, RiskLevel.CRITICAL),
                PageRequest.of(0, 5)).stream().map(mapper::toSummary).toList();
        long demo = scans.count((root, q, cb) -> cb.and(cb.equal(root.get("user").get("id"), userId), cb.isTrue(root.get("demo"))));
        int retention = preferences.findById(userId).map(UserPreference::getRetentionDays).orElse(90);
        return new DashboardSummary(scans.countByUserId(userId), byCategory, byLevel, recent, high, demo,
                scans.oldestScan(userId).orElse(null), retention, NOTE);
    }
}
