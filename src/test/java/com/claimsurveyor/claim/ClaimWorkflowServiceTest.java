package com.claimsurveyor.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.claimsurveyor.ClaimSurveyorApplication;
import com.claimsurveyor.surveyor.Surveyor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ClaimSurveyorApplication.class)
@Transactional
class ClaimWorkflowServiceTest {

    @Autowired
    private ClaimService claimService;

    private Claim newClaim() {
        return claimService.submit(ClaimTestData.validForm());
    }

    private Claim underReviewClaim() {
        Claim claim = newClaim();
        return claimService.assignSurveyor(claim.getId(), null);
    }

    // ---- surveyor assignment ----

    @Test
    void assignMovesClaimToUnderReviewWithSurveyor() {
        Claim claim = claimService.assignSurveyor(newClaim().getId(), null);

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.UNDER_REVIEW);
        assertThat(claim.getSurveyor()).isNotNull();
        assertThat(claim.getAssignedAt()).isNotNull();
    }

    @Test
    void autoAssignSpreadsClaimsAcrossSurveyors() {
        Claim first = underReviewClaim();
        Claim second = underReviewClaim();

        assertThat(second.getSurveyor().getId()).isNotEqualTo(first.getSurveyor().getId());
    }

    @Test
    void assignToChosenSurveyor() {
        Surveyor chosen = claimService.activeSurveyors().get(2);

        Claim claim = claimService.assignSurveyor(newClaim().getId(), chosen.getId());

        assertThat(claim.getSurveyor().getId()).isEqualTo(chosen.getId());
    }

    @Test
    void cannotAssignTwice() {
        Claim claim = underReviewClaim();

        assertThatThrownBy(() -> claimService.assignSurveyor(claim.getId(), null))
                .isInstanceOf(ClaimWorkflowException.class);
    }

    @Test
    void unknownSurveyorIsRejected() {
        Long claimId = newClaim().getId();

        assertThatThrownBy(() -> claimService.assignSurveyor(claimId, 999_999L))
                .isInstanceOf(ClaimWorkflowException.class);
    }

    // ---- reviewer decision ----

    @Test
    void cannotDecideBeforeSurveyorIsAssigned() {
        Long claimId = newClaim().getId();

        assertThatThrownBy(() -> claimService.decide(claimId, ReviewDecision.REJECT, "Not covered by policy"))
                .isInstanceOf(ClaimWorkflowException.class);
    }

    @Test
    void cannotApproveWithoutDocument() {
        Long claimId = underReviewClaim().getId();

        assertThatThrownBy(() -> claimService.decide(claimId, ReviewDecision.APPROVE, null))
                .isInstanceOf(ClaimWorkflowException.class)
                .hasMessageContaining("no supporting document");
    }

    @Test
    void approveWithDocumentSetsApprovedStatus() {
        Long claimId = underReviewClaim().getId();
        claimService.addDocument(claimId, ClaimTestData.png("damage.png"));

        Claim claim = claimService.decide(claimId, ReviewDecision.APPROVE, "Verified by surveyor");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(claim.getDecidedAt()).isNotNull();
        assertThat(claim.getDecisionRemarks()).isEqualTo("Verified by surveyor");
    }

    @Test
    void rejectNeedsReason() {
        Long claimId = underReviewClaim().getId();

        assertThatThrownBy(() -> claimService.decide(claimId, ReviewDecision.REJECT, "no"))
                .isInstanceOf(ClaimWorkflowException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void rejectWithReasonSetsRejectedStatus() {
        Long claimId = underReviewClaim().getId();

        Claim claim = claimService.decide(claimId, ReviewDecision.REJECT, "Policy had lapsed");

        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(claim.getDecisionRemarks()).isEqualTo("Policy had lapsed");
    }

    @Test
    void decidedClaimCannotBeDecidedAgain() {
        Long claimId = underReviewClaim().getId();
        claimService.decide(claimId, ReviewDecision.REJECT, "Policy had lapsed");

        assertThatThrownBy(() -> claimService.decide(claimId, ReviewDecision.REJECT, "Policy had lapsed"))
                .isInstanceOf(ClaimWorkflowException.class);
    }

    @Test
    void reviewQueueShowsOnlyOpenClaims() {
        Claim open = newClaim();
        Claim closed = underReviewClaim();
        claimService.decide(closed.getId(), ReviewDecision.REJECT, "Policy had lapsed");

        assertThat(claimService.findReviewQueue()).extracting(Claim::getId)
                .contains(open.getId())
                .doesNotContain(closed.getId());
    }

    // ---- document validation ----

    @Test
    void validPngIsAccepted() {
        ClaimDocument doc = claimService.addDocument(newClaim().getId(), ClaimTestData.png("photo.png"));

        assertThat(doc.getContentType()).isEqualTo("image/png");
        assertThat(doc.getFileName()).isEqualTo("photo.png");
    }

    @Test
    void validPdfIsAccepted() {
        MockMultipartFile pdf = new MockMultipartFile("file", "bill.pdf", "application/pdf", ClaimTestData.pdfBytes());

        ClaimDocument doc = claimService.addDocument(newClaim().getId(), pdf);

        assertThat(doc.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    void emptyFileIsRejected() {
        Long claimId = newClaim().getId();
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> claimService.addDocument(claimId, empty))
                .isInstanceOf(ClaimWorkflowException.class)
                .hasMessageContaining("choose a file");
    }

    @Test
    void disallowedExtensionIsRejected() {
        Long claimId = newClaim().getId();
        MockMultipartFile exe = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> claimService.addDocument(claimId, exe))
                .isInstanceOf(ClaimWorkflowException.class)
                .hasMessageContaining("Only PDF, JPG and PNG");
    }

    @Test
    void fileWhoseContentDoesNotMatchItsNameIsRejected() {
        Long claimId = newClaim().getId();
        MockMultipartFile fake = new MockMultipartFile("file", "fake.png", "image/png", "just text".getBytes());

        assertThatThrownBy(() -> claimService.addDocument(claimId, fake))
                .isInstanceOf(ClaimWorkflowException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void oversizedFileIsRejected() {
        Long claimId = newClaim().getId();
        byte[] big = new byte[(int) ClaimService.MAX_DOCUMENT_BYTES + 1];
        System.arraycopy(ClaimTestData.pngBytes(), 0, big, 0, 8);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

        assertThatThrownBy(() -> claimService.addDocument(claimId, file))
                .isInstanceOf(ClaimWorkflowException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void documentCannotBeAddedToClosedClaim() {
        Long claimId = underReviewClaim().getId();
        claimService.decide(claimId, ReviewDecision.REJECT, "Policy had lapsed");

        assertThatThrownBy(() -> claimService.addDocument(claimId, ClaimTestData.png("late.png")))
                .isInstanceOf(ClaimWorkflowException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void fileNameIsStrippedOfFolders() {
        assertThat(ClaimService.cleanFileName("C:\\Users\\asha\\photo.png")).isEqualTo("photo.png");
        assertThat(ClaimService.cleanFileName("../../etc/passwd.pdf")).isEqualTo("passwd.pdf");
    }
}
