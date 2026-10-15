package com.universalshield.engine;

/** The five kinds of input UniversalShield can screen. */
public enum ScanCategory {
    MESSAGE("Message"),
    URL("URL / Website"),
    TRANSACTION("Transaction"),
    CALL("Call"),
    APP_PERMISSION("App permissions");

    private final String label;

    ScanCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
