package com.universalshield.repository;

import com.universalshield.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("update AuditEvent a set a.userId = null where a.userId = :userId")
    int detachUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from AuditEvent a where a.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
