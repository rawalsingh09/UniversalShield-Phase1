package com.universalshield.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * The user's own relevant history, supplied to detectors so they can spot unusual
 * behaviour. Only the user's own prior scans are used.
 *
 * @param previousTransactionAmounts amounts of the user's earlier transaction scans
 * @param priorScansOfSubject        earlier scans of the same recipient / domain / number
 * @param priorHighRiskOfSubject     how many of those were HIGH or CRITICAL
 */
public record ScanHistory(List<BigDecimal> previousTransactionAmounts,
                          int priorScansOfSubject, int priorHighRiskOfSubject) {

    public static final ScanHistory EMPTY = new ScanHistory(List.of(), 0, 0);

    public ScanHistory {
        previousTransactionAmounts = List.copyOf(previousTransactionAmounts);
    }
}
