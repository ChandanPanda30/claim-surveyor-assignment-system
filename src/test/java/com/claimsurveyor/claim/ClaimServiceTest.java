package com.claimsurveyor.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claimsurveyor.ClaimSurveyorApplication;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ClaimSurveyorApplication.class)
@Transactional
class ClaimServiceTest {

    @Autowired
    private ClaimService claimService;

    private ClaimForm validForm() {
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
        return form;
    }

    @Test
    void submitSavesClaimWithSubmittedStatusAndClaimNumber() {
        Claim claim = claimService.submit(validForm());

        assertThat(claim.getId()).isNotNull();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(claim.getClaimNumber())
                .isEqualTo(ClaimService.formatClaimNumber(claim.getId()))
                .startsWith("CLM-" + Year.now().getValue() + "-");
    }

    @Test
    void submitTrimsTextFields() {
        ClaimForm form = validForm();
        form.setClaimantName("  Asha Rao  ");

        Claim claim = claimService.submit(form);

        assertThat(claim.getClaimantName()).isEqualTo("Asha Rao");
    }

    @Test
    void findAllReturnsSubmittedClaims() {
        int before = claimService.findAll().size();
        claimService.submit(validForm());
        claimService.submit(validForm());

        assertThat(claimService.findAll()).hasSize(before + 2);
    }

    @Test
    void getThrowsWhenClaimDoesNotExist() {
        assertThatThrownBy(() -> claimService.get(999_999L))
                .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void claimNumberIsZeroPadded() {
        assertThat(ClaimService.formatClaimNumber(7L)).endsWith("-00007");
    }
}
