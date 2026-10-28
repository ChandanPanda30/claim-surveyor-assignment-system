package com.claimsurveyor.tracking;

import com.claimsurveyor.claim.Claim;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Read-only lookup used by the public tracking page. */
public interface ClaimTrackingRepository extends Repository<Claim, Long> {

    Optional<Claim> findByClaimNumber(String claimNumber);
}
