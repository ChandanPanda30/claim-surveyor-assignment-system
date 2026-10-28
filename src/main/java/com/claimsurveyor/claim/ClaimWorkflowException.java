package com.claimsurveyor.claim;

/** Thrown when an action is not allowed for the claim's current state or the input is invalid. */
public class ClaimWorkflowException extends RuntimeException {

    public ClaimWorkflowException(String message) {
        super(message);
    }
}
