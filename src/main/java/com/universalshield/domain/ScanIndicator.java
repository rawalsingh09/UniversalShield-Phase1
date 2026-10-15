package com.universalshield.domain;

import com.universalshield.engine.ResultSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One matched indicator belonging to a {@link Scan}. Deleted together with its scan. */
@Entity
@Table(name = "scan_indicators")
public class ScanIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(name = "indicator_group", nullable = false, length = 60)
    private String group;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 600)
    private String explanation;

    @Column(nullable = false)
    private double weight;

    @Column(name = "detector_id", nullable = false, length = 40)
    private String detectorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultSource source;

    @Column(length = 200)
    private String evidence;

    protected ScanIndicator() {
    }

    public ScanIndicator(String code, String group, String title, String explanation, double weight,
                         String detectorId, ResultSource source, String evidence) {
        this.code = code;
        this.group = group;
        this.title = cut(title, 200);
        this.explanation = cut(explanation, 600);
        this.weight = weight;
        this.detectorId = detectorId;
        this.source = source;
        this.evidence = evidence == null ? null : cut(evidence, 200);
    }

    private static String cut(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    void attachTo(Scan scan) {
        this.scan = scan;
    }

    public String getCode() { return code; }
    public String getGroup() { return group; }
    public String getTitle() { return title; }
    public String getExplanation() { return explanation; }
    public double getWeight() { return weight; }
    public String getDetectorId() { return detectorId; }
    public ResultSource getSource() { return source; }
    public String getEvidence() { return evidence; }
}
