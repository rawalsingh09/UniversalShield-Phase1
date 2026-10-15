package com.universalshield.domain;

import com.universalshield.engine.EvidenceStrength;
import com.universalshield.engine.Recommendation;
import com.universalshield.engine.RiskLevel;
import com.universalshield.engine.ScanCategory;
import com.universalshield.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One stored screening result, owned by exactly one user. */
@Entity
@Table(name = "scans")
public class Scan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public result id used in URLs; the numeric id is never exposed. */
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScanCategory category;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_strength", nullable = false, length = 10)
    private EvidenceStrength evidenceStrength;

    @Column(nullable = false, length = 100)
    private String sources;

    @Column(name = "ruleset_version", nullable = false, length = 30)
    private String rulesetVersion;

    @Column(name = "detector_versions", nullable = false, length = 400)
    private String detectorVersions;

    @Column(name = "input_excerpt", length = 600)
    private String inputExcerpt;

    @Column(name = "subject_hash", length = 64)
    private String subjectHash;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> reasons = new ArrayList<>();

    @Convert(converter = RecommendationListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<Recommendation> recommendations = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> notes = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> disclaimers = new ArrayList<>();

    @Column(nullable = false)
    private boolean demo;

    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weight DESC")
    private List<ScanIndicator> indicators = new ArrayList<>();

    protected Scan() {
    }

    public Scan(UUID publicId, User user, ScanCategory category) {
        this.publicId = publicId;
        this.user = user;
        this.category = category;
    }

    public void addIndicator(ScanIndicator i) {
        i.attachTo(this);
        indicators.add(i);
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public User getUser() { return user; }
    public ScanCategory getCategory() { return category; }
    public int getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public EvidenceStrength getEvidenceStrength() { return evidenceStrength; }
    public String getSources() { return sources; }
    public String getRulesetVersion() { return rulesetVersion; }
    public String getDetectorVersions() { return detectorVersions; }
    public String getInputExcerpt() { return inputExcerpt; }
    public String getSubjectHash() { return subjectHash; }
    public BigDecimal getAmount() { return amount; }
    public List<String> getReasons() { return reasons; }
    public List<Recommendation> getRecommendations() { return recommendations; }
    public List<String> getNotes() { return notes; }
    public List<String> getDisclaimers() { return disclaimers; }
    public boolean isDemo() { return demo; }
    public List<ScanIndicator> getIndicators() { return indicators; }

    public void setResult(int riskScore, RiskLevel riskLevel, EvidenceStrength evidenceStrength, String sources,
                          String rulesetVersion, String detectorVersions) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.evidenceStrength = evidenceStrength;
        this.sources = sources;
        this.rulesetVersion = rulesetVersion;
        this.detectorVersions = detectorVersions;
    }

    public void setExplanations(List<String> reasons, List<Recommendation> recommendations, List<String> notes,
                                List<String> disclaimers) {
        this.reasons = new ArrayList<>(reasons);
        this.recommendations = new ArrayList<>(recommendations);
        this.notes = new ArrayList<>(notes);
        this.disclaimers = new ArrayList<>(disclaimers);
    }

    public void setInputExcerpt(String inputExcerpt) { this.inputExcerpt = inputExcerpt; }
    public void setSubjectHash(String subjectHash) { this.subjectHash = subjectHash; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDemo(boolean demo) { this.demo = demo; }
}
