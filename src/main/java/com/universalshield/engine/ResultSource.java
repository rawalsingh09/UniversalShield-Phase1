package com.universalshield.engine;

/** Where a piece of evidence came from. Shown to the user on every result. */
public enum ResultSource {
    /** Local, deterministic rules shipped with this application. */
    LOCAL_HEURISTIC,
    /** A locally trained statistical model (see docs/ML.md). */
    TRAINED_MODEL,
    /** A verified third-party reputation provider (none configured by default). */
    VERIFIED_EXTERNAL
}
