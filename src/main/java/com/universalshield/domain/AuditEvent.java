package com.universalshield.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Security-relevant event (registration, login success/failure, data deletion). Justified for
 * detecting brute-force attempts and proving deletion requests were honoured. Stores no
 * passwords, no scan content and only a hashed IP address.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(length = 300)
    private String detail;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(Long userId, String eventType, String detail, String ipHash) {
        this.userId = userId;
        this.eventType = eventType;
        this.detail = detail;
        this.ipHash = ipHash;
        this.createdAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public String getEventType() { return eventType; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
