package com.airavat.astralab.core;

public enum StabilityClassification {
    UNSTABLE,
    MARGINALLY_STABLE,
    STABLE,
    VERY_STABLE;

    public static StabilityClassification fromMargin(double staticMarginCalibers) {
        if (staticMarginCalibers < 0.0) {
            return UNSTABLE;
        }
        if (staticMarginCalibers < 1.0) {
            return MARGINALLY_STABLE;
        }
        if (staticMarginCalibers <= 2.0) {
            return STABLE;
        }
        return VERY_STABLE;
    }

    public String displayName() {
        return switch (this) {
            case UNSTABLE -> "UNSTABLE";
            case MARGINALLY_STABLE -> "MARGINALLY STABLE";
            case STABLE -> "STABLE";
            case VERY_STABLE -> "VERY STABLE";
        };
    }
}
