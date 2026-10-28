package com.claimsurveyor.claim;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claimsurveyor.ClaimSurveyorApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ClaimSurveyorApplication.class)
@AutoConfigureMockMvc
@Transactional
class ClaimReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClaimService claimService;

    private Claim newClaim() {
        return claimService.submit(ClaimTestData.validForm());
    }

    @Test
    void reviewQueuePageLoads() throws Exception {
        newClaim();

        mockMvc.perform(get("/review"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Review queue")))
                .andExpect(content().string(containsString("CLM-")));
    }

    @Test
    void detailOfSubmittedClaimOffersSurveyorAssignment() throws Exception {
        Claim claim = newClaim();

        mockMvc.perform(get("/claims/" + claim.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("assign-surveyor")))
                .andExpect(content().string(containsString("Meera Iyer")));
    }

    @Test
    void assigningSurveyorRedirectsAndMovesClaimToReview() throws Exception {
        Claim claim = newClaim();

        mockMvc.perform(post("/claims/" + claim.getId() + "/assign"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("message"));

        mockMvc.perform(get("/claims/" + claim.getId()))
                .andExpect(content().string(containsString("Under review")))
                .andExpect(content().string(containsString("approve-claim")));
    }

    @Test
    void uploadingDocumentShowsItOnTheClaim() throws Exception {
        Claim claim = newClaim();

        mockMvc.perform(multipart("/claims/" + claim.getId() + "/documents")
                        .file(ClaimTestData.png("damage.png")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("message"));

        mockMvc.perform(get("/claims/" + claim.getId()))
                .andExpect(content().string(containsString("damage.png")));
    }

    @Test
    void invalidUploadShowsErrorFlash() throws Exception {
        Claim claim = newClaim();
        MockMultipartFile bad = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/claims/" + claim.getId() + "/documents").file(bad))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void approvingWithoutDocumentShowsError() throws Exception {
        Claim claim = newClaim();
        claimService.assignSurveyor(claim.getId(), null);

        mockMvc.perform(post("/claims/" + claim.getId() + "/decision").param("decision", "APPROVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void fullFlowEndsApproved() throws Exception {
        Claim claim = newClaim();
        claimService.assignSurveyor(claim.getId(), null);
        claimService.addDocument(claim.getId(), ClaimTestData.png("damage.png"));

        mockMvc.perform(post("/claims/" + claim.getId() + "/decision")
                        .param("decision", "APPROVE")
                        .param("remarks", "All documents verified"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("message"));

        mockMvc.perform(get("/claims/" + claim.getId()))
                .andExpect(content().string(containsString("Approved")))
                .andExpect(content().string(containsString("All documents verified")));
    }

    @Test
    void rejectingWithoutReasonShowsError() throws Exception {
        Claim claim = newClaim();
        claimService.assignSurveyor(claim.getId(), null);

        mockMvc.perform(post("/claims/" + claim.getId() + "/decision")
                        .param("decision", "REJECT")
                        .param("remarks", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void uploadedDocumentCanBeDownloaded() throws Exception {
        Claim claim = newClaim();
        ClaimDocument doc = claimService.addDocument(claim.getId(), ClaimTestData.png("damage.png"));

        mockMvc.perform(get("/claims/" + claim.getId() + "/documents/" + doc.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }
}
