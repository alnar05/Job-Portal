package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.service.CandidateService;
import com.narek.jobportal.service.EmployerService;
import com.narek.jobportal.service.ResumeService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAnyRole('EMPLOYER', 'CANDIDATE')")
public class ProfileMvcController {

    private final CandidateService candidateService;
    private final EmployerService employerService;
    private final ResumeService resumeService;

    public ProfileMvcController(CandidateService candidateService,
                                EmployerService employerService,
                                ResumeService resumeService) {
        this.candidateService = candidateService;
        this.employerService = employerService;
        this.resumeService = resumeService;
    }

    @GetMapping("/profile/edit")
    public String editProfile(Authentication authentication, Model model) {
        if (!model.containsAttribute("profileForm")) {
            ProfileUpdateDto profileForm = new ProfileUpdateDto();
            if (hasRole(authentication, "ROLE_EMPLOYER")) {
                Employer employer = employerService.getEmployerByUserEmail(authentication.getName())
                        .orElseThrow(() -> new EntityNotFoundException("Employer profile not found"));
                profileForm.setCompanyName(employer.getCompanyName());
                profileForm.setWebsite(employer.getWebsite());
            } else {
                Candidate candidate = candidateService.getCandidateByUserEmail(authentication.getName())
                        .orElseThrow(() -> new EntityNotFoundException("Candidate profile not found"));
                profileForm.setFullName(candidate.getFullName());
            }
            model.addAttribute("profileForm", profileForm);
        }

        model.addAttribute("isEmployer", hasRole(authentication, "ROLE_EMPLOYER"));
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileUpdateDto profileForm,
                                BindingResult bindingResult,
                                @RequestParam(name = "resumeFile", required = false) MultipartFile resumeFile,
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

        Candidate candidate = candidateService.updateOwnProfile(profileForm, authentication.getName());
        if (resumeFile != null && !resumeFile.isEmpty()) {
            candidate.setResumeFilePath(resumeService.storeResume(resumeFile));
            candidate.setResumeFileName(resumeFile.getOriginalFilename());
            candidate.setParsedResumeSummary(resumeService.parseResumeSummary(resumeFile));
            candidateService.saveCandidate(candidate);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Candidate profile updated successfully.");
        return "redirect:/dashboard/candidate";
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
