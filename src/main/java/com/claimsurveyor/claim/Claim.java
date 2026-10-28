package com.claimsurveyor.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.claimsurveyor.surveyor.Surveyor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String claimNumber;

    @Column(nullable = false)
    private String policyNumber;

    @Column(nullable = false)
    private String claimantName;

    @Column(nullable = false)
    private String claimantEmail;

    @Column(nullable = false)
    private String claimantPhone;

    @Column(nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false)
    private String incidentLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimType claimType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedLoss;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "surveyor_id")
    private Surveyor surveyor;

    private LocalDateTime assignedAt;

    @Column(length = 500)
    private String decisionRemarks;

    private LocalDateTime decidedAt;

    protected Claim() {
        // for JPA
    }

    public Long getId() { return id; }
    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
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
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Surveyor getSurveyor() { return surveyor; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public String getDecisionRemarks() { return decisionRemarks; }
    public LocalDateTime getDecidedAt() { return decidedAt; }

    /** Assigns a surveyor and moves the claim into review. */
    public void assignTo(Surveyor surveyor) {
        this.surveyor = surveyor;
        this.assignedAt = LocalDateTime.now();
        this.status = ClaimStatus.UNDER_REVIEW;
    }

    /** Records the reviewer's final decision. */
    public void decide(ClaimStatus finalStatus, String remarks) {
        this.status = finalStatus;
        this.decisionRemarks = remarks;
        this.decidedAt = LocalDateTime.now();
    }

    /** Factory used by the service so callers cannot create a half-filled claim. */
    public static Claim create() {
        return new Claim();
    }
}
