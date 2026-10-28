package com.claimsurveyor.tracking;

import com.claimsurveyor.claim.ClaimStatus;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TrackingController {

    private final ClaimTrackingRepository repository;

    public TrackingController(ClaimTrackingRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/track")
    public String track(@RequestParam(required = false) String claimNumber, Model model) {
        String entered = claimNumber == null ? "" : claimNumber.trim();
        model.addAttribute("claimNumber", entered);
        if (!entered.isEmpty()) {
            repository.findByClaimNumber(entered.toUpperCase(Locale.ROOT)).ifPresentOrElse(
                    claim -> {
                        model.addAttribute("claim", claim);
                        model.addAttribute("stage", stageOf(claim.getStatus()));
                    },
                    () -> model.addAttribute("notFound", true));
        }
        return "track";
    }

    /** 1 = submitted, 2 = under review, 3 = decision made. */
    static int stageOf(ClaimStatus status) {
        return switch (status) {
            case SUBMITTED -> 1;
            case UNDER_REVIEW -> 2;
            case APPROVED, REJECTED -> 3;
        };
    }
}
