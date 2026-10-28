package com.claimsurveyor.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Critical user journeys, driven through a real browser. */
class ClaimJourneysIT extends BaseSeleniumTest {

    /** A tiny file that starts with the PNG signature, which is all the upload validation checks. */
    private Path createEvidenceImage() throws IOException {
        Path file = Files.createTempFile("damage-photo", ".png");
        Files.write(file, new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
        file.toFile().deleteOnExit();
        return file;
    }

    // Journey 1: claimant submits a valid claim
    @Test
    void claimantSubmitsValidClaimAndSeesConfirmation() {
        String claimNumber = submitValidClaim();

        assertThat(claimNumber).matches("CLM-\\d{4}-\\d{5}");
        assertThat(text("flash-message")).contains("Claim submitted successfully").contains(claimNumber);
        assertThat(text("claim-status")).isEqualTo("Submitted");
        assertThat(text("claim-surveyor")).isEqualTo("Not assigned yet");
    }

    // Journey 2: invalid data is rejected and nothing is saved
    @Test
    void invalidClaimShowsValidationErrorsAndIsNotSaved() {
        open("/claims/new");
        click("submit-claim");

        assertThat(text("policyNumber-error")).contains("Policy number is required");
        assertThat(text("claimantName-error")).contains("Claimant name is required");
        assertThat(text("claimType-error")).contains("Select a claim type");
        assertThat(exists("claim-number")).isFalse();

        fillClaimForm("12345", LocalDate.now().minusDays(1));
        click("submit-claim");
        assertThat(text("policyNumber-error")).contains("must look like POL-123456");

        fillClaimForm("POL-123456", LocalDate.now().plusDays(10));
        click("submit-claim");
        assertThat(text("incidentDate-error")).contains("cannot be in the future");
        assertThat(exists("claim-number")).isFalse();
    }

    // Journey 3: reviewer assigns a surveyor, checks the document and approves
    @Test
    void reviewerAssignsSurveyorUploadsDocumentAndApproves() throws IOException {
        submitValidClaim();

        click("assign-surveyor");
        waitForText("claim-status", "Under review");
        assertThat(text("flash-message")).contains("Surveyor assigned");
        assertThat(text("claim-surveyor")).isNotEqualTo("Not assigned yet");

        Path image = createEvidenceImage();
        find("document-file").sendKeys(image.toAbsolutePath().toString());
        click("upload-document");
        assertThat(text("document-list")).contains(image.getFileName().toString());

        click("approve-claim");
        waitForText("claim-status", "Approved");
    }

    // Journey 4: a rejection needs a reason
    @Test
    void rejectionRequiresReason() {
        submitValidClaim();
        click("assign-surveyor");
        waitForText("claim-status", "Under review");

        click("reject-claim");
        assertThat(text("flash-error")).contains("reason for rejection");
        assertThat(text("claim-status")).isEqualTo("Under review");

        type("remarks", "Policy had lapsed before the incident date");
        click("reject-claim");
        waitForText("claim-status", "Rejected");
        assertThat(text("claim-remarks")).contains("Policy had lapsed");
    }

    // Journey 5: anyone can track a claim by its number
    @Test
    void claimCanBeTrackedByClaimNumber() {
        String claimNumber = submitValidClaim();

        open("/track");
        type("claimNumber", claimNumber);
        click("track-claim");
        assertThat(text("track-number")).isEqualTo(claimNumber);
        assertThat(text("track-status")).isEqualTo("Submitted");

        open("/track?claimNumber=CLM-0000-00000");
        assertThat(text("track-not-found")).contains("No claim found");
    }
}
