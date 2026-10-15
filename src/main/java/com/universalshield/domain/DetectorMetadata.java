package com.universalshield.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Record of each detector version that has run on this installation, so stored results can
 * always be traced to the detector version that produced them.
 */
@Entity
@Table(name = "detector_metadata")
public class DetectorMetadata {

    @Id
    @Column(name = "detector_id", length = 40)
    private String detectorId;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false, length = 40)
    private String version;

    @Column(nullable = false, length = 120)
    private String categories;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(nullable = false, length = 600)
    private String description;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DetectorMetadata() {
    }

    public DetectorMetadata(String detectorId) {
        this.detectorId = detectorId;
    }

    public void update(String displayName, String version, String categories, String sourceType, String description) {
        this.displayName = displayName;
        this.version = version;
        this.categories = categories;
        this.sourceType = sourceType;
        this.description = description.length() > 600 ? description.substring(0, 600) : description;
        this.updatedAt = Instant.now();
    }

    public String getDetectorId() { return detectorId; }
    public String getDisplayName() { return displayName; }
    public String getVersion() { return version; }
    public String getCategories() { return categories; }
    public String getSourceType() { return sourceType; }
    public String getDescription() { return description; }
    public Instant getUpdatedAt() { return updatedAt; }
}
