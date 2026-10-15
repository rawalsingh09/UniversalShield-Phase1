package com.universalshield.engine;

/**
 * How much independent evidence supports the assessment. Deliberately separate from the
 * risk score: a score of 40 backed by three independent signal groups is better supported
 * than a score of 40 from one keyword in a ten-character message.
 */
public enum EvidenceStrength {
    LIMITED, MODERATE, STRONG
}
