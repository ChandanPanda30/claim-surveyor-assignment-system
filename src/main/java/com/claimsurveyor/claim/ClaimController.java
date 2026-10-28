package com.claimsurveyor.claim;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
        model.addAttribute("claim", claimService.get(id));
        return "claim-detail";
    }
}
