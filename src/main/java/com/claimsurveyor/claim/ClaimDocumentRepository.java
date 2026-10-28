package com.claimsurveyor.claim;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {

    List<ClaimDocument> findByClaimIdOrderByUploadedAtDesc(Long claimId);

    long countByClaimId(Long claimId);

    Optional<ClaimDocument> findByIdAndClaimId(Long id, Long claimId);
}
