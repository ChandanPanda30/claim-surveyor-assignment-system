package com.claimsurveyor.claim;

public enum ClaimType {
    MOTOR("Motor"),
    FIRE("Fire"),
    THEFT("Theft"),
    FLOOD("Flood / Water damage"),
    HEALTH("Health");

    private final String label;

    ClaimType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
