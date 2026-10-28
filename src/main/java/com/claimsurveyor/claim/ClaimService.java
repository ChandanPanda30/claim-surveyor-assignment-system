package com.claimsurveyor.claim;

import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimService {

    private final ClaimRepository repository;

    public ClaimService(ClaimRepository repository) {
        this.repository = repository;
    }

    /** Saves a new claim in SUBMITTED status and gives it a readable claim number. */
    @Transactional
    public Claim submit(ClaimForm form) {
        Claim claim = Claim.create();
        claim.setPolicyNumber(form.getPolicyNumber().trim());
        claim.setClaimantName(form.getClaimantName().trim());
        claim.setClaimantEmail(form.getClaimantEmail().trim());
        claim.setClaimantPhone(form.getClaimantPhone().trim());
        claim.setIncidentDate(form.getIncidentDate());
        claim.setIncidentLocation(form.getIncidentLocation().trim());
        claim.setClaimType(form.getClaimType());
        claim.setEstimatedLoss(form.getEstimatedLoss());
        claim.setDescription(form.getDescription().trim());
        claim.setStatus(ClaimStatus.SUBMITTED);

        Claim saved = repository.save(claim);
        saved.setClaimNumber(formatClaimNumber(saved.getId()));
        return repository.save(saved);
    }

    @Transactional(readOnly = true)
    public List<Claim> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Claim get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
    }

    static String formatClaimNumber(Long id) {
        return String.format("CLM-%d-%05d", Year.now().getValue(), id);
    }
}
