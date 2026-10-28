package com.claimsurveyor.claim;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claimsurveyor.ClaimSurveyorApplication;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ClaimSurveyorApplication.class)
@AutoConfigureMockMvc
@Transactional
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpServletRequestBuilder validSubmission() {
        return submissionWith(null, null);
    }

    /**
     * Builds a valid submission, replacing one field's value. MockMvc's param()
     * adds a
     * second value instead of replacing, so overrides must be applied before
     * building.
     */
    private MockHttpServletRequestBuilder submissionWith(String field, String value) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("policyNumber", "POL-123456");
        fields.put("claimantName", "Asha Rao");
        fields.put("claimantEmail", "asha@example.com");
        fields.put("claimantPhone", "9876543210");
        fields.put("incidentDate", LocalDate.now().minusDays(1).toString());
        fields.put("incidentLocation", "Andheri, Mumbai");
        fields.put("claimType", "MOTOR");
        fields.put("estimatedLoss", "45000.00");
        fields.put("description", "Rear bumper damaged in a parking collision.");
        if (field != null) {
            fields.put(field, value);
        }
        MockHttpServletRequestBuilder request = post("/claims");
        fields.forEach(request::param);
        return request;
    }

    @Test
    void formPageLoads() throws Exception {
        mockMvc.perform(get("/claims/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Submit a claim")));
    }

    @Test
    void validSubmissionRedirectsToClaimDetail() throws Exception {
        mockMvc.perform(validSubmission())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/claims/*"));
    }

    @Test
    void emptySubmissionShowsValidationErrors() throws Exception {
        mockMvc.perform(post("/claims"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Policy number is required")))
                .andExpect(content().string(containsString("Claimant name is required")))
                .andExpect(content().string(containsString("Select a claim type")));
    }

    @Test
    void badPolicyNumberIsRejected() throws Exception {
        mockMvc.perform(submissionWith("policyNumber", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Policy number must look like POL-123456")));
    }

    @Test
    void futureIncidentDateIsRejected() throws Exception {
        mockMvc.perform(submissionWith("incidentDate", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Incident date cannot be in the future")));
    }

    @Test
    void invalidPhoneIsRejected() throws Exception {
        mockMvc.perform(submissionWith("claimantPhone", "12345"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("10-digit")));
    }

    @Test
    void claimListPageLoads() throws Exception {
        mockMvc.perform(get("/claims"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("All claims")));
    }

    @Test
    void unknownClaimReturns404() throws Exception {
        mockMvc.perform(get("/claims/999999"))
                .andExpect(status().isNotFound());
    }
}