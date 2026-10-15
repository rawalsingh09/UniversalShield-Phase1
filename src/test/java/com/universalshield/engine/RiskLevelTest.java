package com.universalshield.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskLevelTest {

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({"0,LOW", "29,LOW", "30,MEDIUM", "59,MEDIUM", "60,HIGH", "84,HIGH", "85,CRITICAL", "100,CRITICAL"})
    void defaultThresholdBoundaries(int score, RiskLevel expected) {
        assertThat(RiskLevel.fromScore(score, RiskThresholds.DEFAULT)).isEqualTo(expected);
    }

    @Test
    void customThresholdsAreRespected() {
        RiskThresholds t = new RiskThresholds(10, 20, 30);
        assertThat(RiskLevel.fromScore(9, t)).isEqualTo(RiskLevel.LOW);
        assertThat(RiskLevel.fromScore(10, t)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(RiskLevel.fromScore(20, t)).isEqualTo(RiskLevel.HIGH);
        assertThat(RiskLevel.fromScore(30, t)).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void scoresOutsideRangeAreRejected() {
        assertThatThrownBy(() -> RiskLevel.fromScore(-1, RiskThresholds.DEFAULT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RiskLevel.fromScore(101, RiskThresholds.DEFAULT)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inconsistentThresholdsAreRejected() {
        assertThatThrownBy(() -> new RiskThresholds(60, 30, 85)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RiskThresholds(0, 30, 85)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RiskThresholds(30, 60, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void indicatorWeightMustBeInRange() {
        assertThatThrownBy(() -> new Indicator("c", "g", "t", "e", 0, "d", ResultSource.LOCAL_HEURISTIC, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Indicator("c", "g", "t", "e", 1.01, "d", ResultSource.LOCAL_HEURISTIC, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
