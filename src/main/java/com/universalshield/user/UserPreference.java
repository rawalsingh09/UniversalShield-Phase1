package com.universalshield.user;

import com.universalshield.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Per-user privacy and convenience settings. One row per user (user_id is the primary key). */
@Entity
@Table(name = "user_preferences")
public class UserPreference extends Auditable {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "store_input_excerpts", nullable = false)
    private boolean storeInputExcerpts;

    @Column(name = "default_category", nullable = false, length = 30)
    private String defaultCategory;

    protected UserPreference() {
    }

    public UserPreference(Long userId, int retentionDays) {
        this.userId = userId;
        this.retentionDays = retentionDays;
        this.storeInputExcerpts = true;
        this.defaultCategory = "MESSAGE";
    }

    public Long getUserId() { return userId; }
    public int getRetentionDays() { return retentionDays; }
    public boolean isStoreInputExcerpts() { return storeInputExcerpts; }
    public String getDefaultCategory() { return defaultCategory; }

    public void update(int retentionDays, boolean storeInputExcerpts, String defaultCategory) {
        this.retentionDays = retentionDays;
        this.storeInputExcerpts = storeInputExcerpts;
        this.defaultCategory = defaultCategory;
    }
}
