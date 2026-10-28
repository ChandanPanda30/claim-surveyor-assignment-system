package com.claimsurveyor.claim;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findAllByOrderByCreatedAtDesc();

    List<Claim> findByStatusInOrderByCreatedAtAsc(Collection<ClaimStatus> statuses);

    long countBySurveyorIdAndStatus(Long surveyorId, ClaimStatus status);
}
