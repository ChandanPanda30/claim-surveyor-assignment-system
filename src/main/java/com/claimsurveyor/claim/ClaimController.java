package com.claimsurveyor.claim;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping("/claims/new")
    public String newClaim(Model model) {
        model.addAttribute("claimForm", new ClaimForm());
        model.addAttribute("claimTypes", ClaimType.values());
        return "claim-form";
    }

    @PostMapping("/claims")
    public String submit(@Valid ClaimForm claimForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("claimTypes", ClaimType.values());
            return "claim-form";
        }
        Claim claim = claimService.submit(claimForm);
        redirectAttributes.addFlashAttribute("message",
                "Claim submitted successfully. Your claim number is " + claim.getClaimNumber());
        return "redirect:/claims/" + claim.getId();
    }

    @GetMapping("/claims")
    public String list(Model model) {
        model.addAttribute("claims", claimService.findAll());
        return "claim-list";
    }

    @GetMapping("/claims/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Claim claim = claimService.get(id);
        boolean open = claim.getStatus() == ClaimStatus.SUBMITTED || claim.getStatus() == ClaimStatus.UNDER_REVIEW;
        model.addAttribute("claim", claim);
        model.addAttribute("documents", claimService.documentsFor(id));
        model.addAttribute("surveyors", claimService.activeSurveyors());
        model.addAttribute("canUpload", open);
        return "claim-detail";
    }

    @GetMapping("/review")
    public String reviewQueue(Model model) {
        model.addAttribute("claims", claimService.findReviewQueue());
        return "review-queue";
    }

    @PostMapping("/claims/{id}/documents")
    public String uploadDocument(@PathVariable Long id,
                                 @RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            ClaimDocument document = claimService.addDocument(id, file);
            redirectAttributes.addFlashAttribute("message", "Document uploaded: " + document.getFileName());
        } catch (ClaimWorkflowException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/claims/" + id;
    }

    @GetMapping("/claims/{id}/documents/{documentId}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, @PathVariable Long documentId) {
        ClaimDocument document = claimService.getDocument(id, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getFileName()).build().toString())
                .body(document.getData());
    }

    @PostMapping("/claims/{id}/assign")
    public String assign(@PathVariable Long id,
                         @RequestParam(required = false) Long surveyorId,
                         RedirectAttributes redirectAttributes) {
        try {
            Claim claim = claimService.assignSurveyor(id, surveyorId);
            redirectAttributes.addFlashAttribute("message",
                    "Surveyor assigned: " + claim.getSurveyor().getName() + ". The claim is now under review.");
        } catch (ClaimWorkflowException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/claims/" + id;
    }

    @PostMapping("/claims/{id}/decision")
    public String decide(@PathVariable Long id,
                         @RequestParam ReviewDecision decision,
                         @RequestParam(required = false) String remarks,
                         RedirectAttributes redirectAttributes) {
        try {
            Claim claim = claimService.decide(id, decision, remarks);
            redirectAttributes.addFlashAttribute("message",
                    "Claim " + claim.getStatus().getLabel().toLowerCase() + ".");
        } catch (ClaimWorkflowException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/claims/" + id;
    }
}
