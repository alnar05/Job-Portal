package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.service.CandidateService;
import com.narek.jobportal.service.EmployerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAnyRole('EMPLOYER', 'CANDIDATE')")
public class ProfileMvcController {

    private final CandidateService candidateService;
    private final EmployerService employerService;

    public ProfileMvcController(CandidateService candidateService, EmployerService employerService) {
        this.candidateService = candidateService;
        this.employerService = employerService;
    }

    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model) {
        if (!model.containsAttribute("profileForm")) {
            ProfileUpdateDto profileForm = new ProfileUpdateDto();
            if (hasRole(authentication, "ROLE_EMPLOYER")) {
                Employer employer = employerService.getEmployerByUserEmail(authentication.getName())
                        .orElseThrow(() -> new RuntimeException("Employer profile not found"));
                profileForm.setCompanyName(employer.getCompanyName());
                profileForm.setWebsite(employer.getWebsite());
            } else {
                Candidate candidate = candidateService.getCandidateByUserEmail(authentication.getName())
                        .orElseThrow(() -> new RuntimeException("Candidate profile not found"));
                profileForm.setFullName(candidate.getFullName());
                profileForm.setResumeUrl(candidate.getResumeUrl());
            }
            model.addAttribute("profileForm", profileForm);
        }

        model.addAttribute("isEmployer", hasRole(authentication, "ROLE_EMPLOYER"));
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileUpdateDto profileForm,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEmployer", hasRole(authentication, "ROLE_EMPLOYER"));
            return "profile/edit";
        }

        if (hasRole(authentication, "ROLE_EMPLOYER")) {
            employerService.updateOwnProfile(profileForm, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Employer profile updated successfully.");
            return "redirect:/dashboard/employer";
        }

        candidateService.updateOwnProfile(profileForm, authentication.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Candidate profile updated successfully.");
        return "redirect:/dashboard/candidate";
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}