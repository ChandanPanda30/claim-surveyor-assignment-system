package com.claimsurveyor.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claimsurveyor.ClaimSurveyorApplication;
import com.claimsurveyor.claim.Claim;
import com.claimsurveyor.claim.ClaimForm;
import com.claimsurveyor.claim.ClaimService;
import com.claimsurveyor.claim.ClaimStatus;
import com.claimsurveyor.claim.ClaimType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ClaimSurveyorApplication.class)
@AutoConfigureMockMvc
@Transactional
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClaimService claimService;

    private Claim newClaim() {
        ClaimForm form = new ClaimForm();
        form.setPolicyNumber("POL-123456");
        form.setClaimantName("Asha Rao");
        form.setClaimantEmail("asha@example.com");
        form.setClaimantPhone("9876543210");
        form.setIncidentDate(LocalDate.now().minusDays(2));
        form.setIncidentLocation("Andheri, Mumbai");
        form.setClaimType(ClaimType.MOTOR);
        form.setEstimatedLoss(new BigDecimal("45000.00"));
        form.setDescription("Rear bumper damaged in a parking collision.");
        return claimService.submit(form);
    }

    @Test
    void trackPageLoadsWithoutResult() throws Exception {
        mockMvc.perform(get("/track"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Track your claim")))
                .andExpect(content().string(not(containsString("track-result"))));
    }

    @Test
    void unknownClaimNumberShowsNotFound() throws Exception {
        mockMvc.perform(get("/track").param("claimNumber", "CLM-0000-00000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No claim found")));
    }

    @Test
    void knownClaimNumberShowsStatus() throws Exception {
        Claim claim = newClaim();

        mockMvc.perform(get("/track").param("claimNumber", claim.getClaimNumber()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(claim.getClaimNumber())))
                .andExpect(content().string(containsString("Submitted")))
                .andExpect(content().string(not(containsString("No claim found"))));
    }

    @Test
    void claimNumberLookupIgnoresCaseAndSpaces() throws Exception {
        Claim claim = newClaim();

        mockMvc.perform(get("/track").param("claimNumber", "  " + claim.getClaimNumber().toLowerCase() + " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("track-result")));
    }

    @Test
    void stageReflectsStatus() {
        assertThat(TrackingController.stageOf(ClaimStatus.SUBMITTED)).isEqualTo(1);
        assertThat(TrackingController.stageOf(ClaimStatus.UNDER_REVIEW)).isEqualTo(2);
        assertThat(TrackingController.stageOf(ClaimStatus.APPROVED)).isEqualTo(3);
        assertThat(TrackingController.stageOf(ClaimStatus.REJECTED)).isEqualTo(3);
    }
}
