package com.claimsurveyor.claim;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Data entered on the claim submission form, with validation rules. */
public class ClaimForm {

    @NotBlank(message = "Policy number is required")
    @Pattern(regexp = "^$|POL-\\d{6}", message = "Policy number must look like POL-123456")
    private String policyNumber;

    @NotBlank(message = "Claimant name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String claimantName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String claimantEmail;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^$|[6-9]\\d{9}", message = "Phone must be a 10-digit Indian mobile number")
    private String claimantPhone;

    @NotNull(message = "Incident date is required")
    @PastOrPresent(message = "Incident date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate incidentDate;

    @NotBlank(message = "Incident location is required")
    @Size(max = 200, message = "Location must be at most 200 characters")
    private String incidentLocation;

    @NotNull(message = "Select a claim type")
    private ClaimType claimType;

    @NotNull(message = "Estimated loss is required")
    @DecimalMin(value = "1.00", message = "Estimated loss must be at least 1")
    @DecimalMax(value = "10000000.00", message = "Estimated loss cannot exceed 1,00,00,000")
    private BigDecimal estimatedLoss;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getClaimantName() { return claimantName; }
    public void setClaimantName(String claimantName) { this.claimantName = claimantName; }
    public String getClaimantEmail() { return claimantEmail; }
    public void setClaimantEmail(String claimantEmail) { this.claimantEmail = claimantEmail; }
    public String getClaimantPhone() { return claimantPhone; }
    public void setClaimantPhone(String claimantPhone) { this.claimantPhone = claimantPhone; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }
    public String getIncidentLocation() { return incidentLocation; }
    public void setIncidentLocation(String incidentLocation) { this.incidentLocation = incidentLocation; }
    public ClaimType getClaimType() { return claimType; }
    public void setClaimType(ClaimType claimType) { this.claimType = claimType; }
    public BigDecimal getEstimatedLoss() { return estimatedLoss; }
    public void setEstimatedLoss(BigDecimal estimatedLoss) { this.estimatedLoss = estimatedLoss; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
