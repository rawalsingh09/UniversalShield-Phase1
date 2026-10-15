package com.universalshield.ml;

/** Text-classification tasks supported by the optional ML module. */
public enum MlTask {
    MESSAGE, URL;

    public String dirName() {
        return name().toLowerCase();
    }
}
