package com.universalshield.repository;

import com.universalshield.domain.Scan;
import com.universalshield.domain.ScanIndicator;
import com.universalshield.engine.EvidenceStrength;
import com.universalshield.engine.Recommendation;
import com.universalshield.engine.ResultSource;
import com.universalshield.engine.RiskLevel;
import com.universalshield.engine.ScanCategory;
import com.universalshield.user.User;
import com.universalshield.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies the Flyway schema, JPA mappings, constraints and cascades on H2. */
@DataJpaTest
@ActiveProfiles("test")
class PersistenceTest {

    @Autowired UserRepository users;
    @Autowired ScanRepository scans;
    @Autowired EntityManager em;

    private Scan scan(User u, int score, RiskLevel level) {
        Scan s = new Scan(UUID.randomUUID(), u, ScanCategory.TRANSACTION);
        s.setResult(score, level, EvidenceStrength.MODERATE, "LOCAL_HEURISTIC", "r1", "transaction-heuristic=1.0.0");
        s.setExplanations(List.of("reason"), List.of(new Recommendation("act", "why", 1)), List.of("note"), List.of("disc"));
        s.setAmount(new BigDecimal("123.45"));
        s.setSubjectHash("abc");
        s.addIndicator(new ScanIndicator("C", "G", "T", "E", 0.5, "d", ResultSource.LOCAL_HEURISTIC, "ev"));
        return scans.save(s);
    }

    @Test
    void scanRoundTripsWithJsonColumnsAndIndicators() {
        User u = users.save(new User("p1@example.test", "P", "{noop}x"));
        Scan saved = scan(u, 50, RiskLevel.MEDIUM);
        em.flush();
        em.clear();
        Scan loaded = scans.findByPublicIdAndUserId(saved.getPublicId(), u.getId()).orElseThrow();
        assertThat(loaded.getRecommendations()).extracting(Recommendation::action).containsExactly("act");
        assertThat(loaded.getReasons()).containsExactly("reason");
        assertThat(loaded.getIndicators()).hasSize(1);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(scans.recentAmounts(u.getId(), ScanCategory.TRANSACTION, PageRequest.of(0, 5))).containsExactly(new BigDecimal("123.45"));
        assertThat(scans.countByUserIdAndSubjectHashAndRiskLevelIn(u.getId(), "abc", List.of(RiskLevel.MEDIUM))).isEqualTo(1);
    }

    @Test
    void deletingUserCascadesToScansAndIndicators() {
        User u = users.save(new User("p2@example.test", "P", "{noop}x"));
        scan(u, 10, RiskLevel.LOW);
        em.flush();
        em.clear();
        em.createNativeQuery("DELETE FROM users WHERE id = " + u.getId()).executeUpdate();
        assertThat(((Number) em.createNativeQuery("SELECT COUNT(*) FROM scan_indicators").getSingleResult()).longValue()).isZero();
        assertThat(scans.countByUserId(u.getId())).isZero();
    }

    @Test
    void databaseConstraintsRejectInvalidData() {
        users.save(new User("dup@example.test", "A", "{noop}x"));
        assertThatThrownBy(() -> {
            users.save(new User("dup@example.test", "B", "{noop}x"));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void riskScoreCheckConstraint() {
        User u = users.save(new User("p3@example.test", "P", "{noop}x"));
        em.flush();
        assertThatThrownBy(() -> em.createNativeQuery("INSERT INTO scans (public_id, user_id, category, risk_score, risk_level, "
                + "evidence_strength, sources, ruleset_version, detector_versions, reasons, recommendations, notes, disclaimers, created_at, updated_at) "
                + "VALUES (RANDOM_UUID(), " + u.getId() + ", 'URL', 150, 'LOW', 'LIMITED', 'x', 'x', 'x', '[]', '[]', '[]', '[]', NOW(), NOW())")
                .executeUpdate()).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }
}
