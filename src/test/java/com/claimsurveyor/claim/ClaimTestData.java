package com.claimsurveyor.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.mock.web.MockMultipartFile;

/** Shared builders for tests. */
final class ClaimTestData {

    private ClaimTestData() {
    }

    static ClaimForm validForm() {
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

    static byte[] pngBytes() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }

    static byte[] pdfBytes() {
        return "%PDF-1.4 test".getBytes();
    }

    static MockMultipartFile png(String name) {
        return new MockMultipartFile("file", name, "image/png", pngBytes());
    }
}
