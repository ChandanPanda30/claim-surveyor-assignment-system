package com.claimsurveyor.claim;

public enum ClaimStatus {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under review"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    ClaimStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
