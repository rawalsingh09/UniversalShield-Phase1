package com.universalshield.repository;

import com.universalshield.domain.Scan;
import com.universalshield.engine.RiskLevel;
import com.universalshield.engine.ScanCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Every query is scoped by user id so one user can never read another user's scans.
 * All queries are parameterised (no string concatenation), preventing SQL injection.
 */
public interface ScanRepository extends JpaRepository<Scan, Long>, JpaSpecificationExecutor<Scan> {

    Optional<Scan> findByPublicIdAndUserId(UUID publicId, Long userId);

    long countByUserId(Long userId);

    @Query("select s.category, count(s) from Scan s where s.user.id = :userId group by s.category")
    List<Object[]> countByCategory(@Param("userId") Long userId);

    @Query("select s.riskLevel, count(s) from Scan s where s.user.id = :userId group by s.riskLevel")
    List<Object[]> countByLevel(@Param("userId") Long userId);

    List<Scan> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Scan> findByUserIdAndRiskLevelInOrderByCreatedAtDesc(Long userId, Collection<RiskLevel> levels, Pageable pageable);

    @Query("select s.amount from Scan s where s.user.id = :userId and s.category = :category and s.amount is not null "
            + "order by s.createdAt desc")
    List<BigDecimal> recentAmounts(@Param("userId") Long userId, @Param("category") ScanCategory category, Pageable pageable);

    long countByUserIdAndSubjectHash(Long userId, String subjectHash);

    long countByUserIdAndSubjectHashAndRiskLevelIn(Long userId, String subjectHash, Collection<RiskLevel> levels);

    @Modifying
    @Query("delete from Scan s where s.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Query("select s.id from Scan s where s.user.id = :userId and s.createdAt < :cutoff")
    List<Long> findIdsOlderThan(@Param("userId") Long userId, @Param("cutoff") Instant cutoff);

    @Query("select min(s.createdAt) from Scan s where s.user.id = :userId")
    Optional<Instant> oldestScan(@Param("userId") Long userId);
}
