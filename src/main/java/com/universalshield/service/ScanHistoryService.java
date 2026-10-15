package com.universalshield.service;

import com.universalshield.engine.RiskLevel;
import com.universalshield.engine.ScanCategory;
import com.universalshield.engine.ScanHistory;
import com.universalshield.repository.ScanRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** Builds the {@link ScanHistory} context from the user's own earlier scans only. */
@Service
public class ScanHistoryService {

    static final int MAX_AMOUNTS = 50;
    private static final Set<RiskLevel> HIGH = Set.of(RiskLevel.HIGH, RiskLevel.CRITICAL);

    private final ScanRepository scans;

    public ScanHistoryService(ScanRepository scans) {
        this.scans = scans;
    }

    public ScanHistory forUser(Long userId, ScanCategory category, String subjectHash) {
        List<BigDecimal> amounts = category == ScanCategory.TRANSACTION
                ? scans.recentAmounts(userId, ScanCategory.TRANSACTION, PageRequest.of(0, MAX_AMOUNTS))
                : List.of();
        int prior = 0;
        int priorHigh = 0;
        if (subjectHash != null) {
            prior = (int) scans.countByUserIdAndSubjectHash(userId, subjectHash);
            priorHigh = (int) scans.countByUserIdAndSubjectHashAndRiskLevelIn(userId, subjectHash, HIGH);
        }
        return new ScanHistory(amounts, prior, priorHigh);
    }
}
