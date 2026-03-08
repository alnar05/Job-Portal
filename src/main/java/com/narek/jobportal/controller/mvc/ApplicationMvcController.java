package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.BulkApplicationActionDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.exception.JobApplicationClosedException;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.CandidateService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.service.ResumeService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/applications")
public class ApplicationMvcController {

    private final ApplicationService applicationService;
    private final AuthService authService;
    private final JobService jobService;
    private final ResumeService resumeService;
    private final CandidateService candidateService;
    private final Validator validator;

    public ApplicationMvcController(ApplicationService applicationService,
                                    AuthService authService,
                                    JobService jobService,
                                    ResumeService resumeService,
                                    CandidateService candidateService,
                                    Validator validator) {
        this.applicationService = applicationService;
        this.authService = authService;
        this.jobService = jobService;
        this.resumeService = resumeService;
        this.candidateService = candidateService;
        this.validator = validator;
    }

    @PostMapping("/apply/{jobId}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String applyForJob(@PathVariable Long jobId,
                              @RequestParam(name = "coverLetter", required = false) String coverLetter,
                              @RequestParam(name = "resumeFile", required = false) MultipartFile resumeFile,
                              RedirectAttributes redirectAttributes) {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto();
        dto.setJobId(jobId);
        dto.setCoverLetter(coverLetter);

        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            ConstraintViolation<ApplicationCreateUpdateDto> violation = violations.iterator().next();
            redirectAttributes.addFlashAttribute("errorMessage", violation.getMessage());
            return "redirect:/jobs/" + jobId;
        }

        if (resumeFile != null && !resumeFile.isEmpty()) {
            Candidate candidate = authService.getCurrentCandidate();
            candidate.setResumeFilePath(resumeService.storeResume(resumeFile));
            candidate.setResumeFileName(resumeFile.getOriginalFilename());
            candidate.setParsedResumeSummary(resumeService.parseResumeSummary(resumeFile));
            candidateService.saveCandidate(candidate);
        }

        try {
            applicationService.createApplication(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Application submitted successfully.");
        } catch (JobApplicationClosedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }

        return "redirect:/jobs/" + jobId;
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('EMPLOYER','ADMIN')")
    public String bulkAction(@ModelAttribute BulkApplicationActionDto dto,
                             @RequestParam Long jobId,
                             RedirectAttributes redirectAttributes) {
        applicationService.bulkUpdateStatus(dto.getApplicationIds(), dto.getAccept(), dto.getInternalNotes());
        redirectAttributes.addFlashAttribute("successMessage", "Bulk update completed.");
        return "redirect:/applications/job/" + jobId;
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('EMPLOYER','ADMIN')")
    public String addNotes(@PathVariable Long id,
                           @RequestParam String internalNotes,
                           RedirectAttributes redirectAttributes) {
        applicationService.addInternalNotes(id, internalNotes);
        redirectAttributes.addFlashAttribute("successMessage", "Internal notes updated.");
        return "redirect:/applications/" + id;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String myApplications(Model model) {
        Candidate currentCandidate = authService.getCurrentCandidate();
        model.addAttribute("applications", applicationService.getApplicationsByCandidateId(currentCandidate.getId()));
        return "applications/my-applications";
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public String applicationsForJob(@PathVariable Long jobId, Model model) {
        JobResponseDto job = jobService.getJobById(jobId);
        model.addAttribute("job", job);
        model.addAttribute("applications", applicationService.getApplicationsByJobId(jobId));
        return "applications/job-applications";
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#id) or @authService.isCurrentCandidateApplication(#id)")
    public String applicationDetails(@PathVariable Long id,
                                     Authentication authentication,
                                     Model model) {
        boolean isEmployerOrAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYER") || a.getAuthority().equals("ROLE_ADMIN"));

        if (isEmployerOrAdmin) {
            model.addAttribute("applicationDetails", applicationService.markAsReviewed(id));
            model.addAttribute("backUrl", "/employer/dashboard");
            model.addAttribute("backLabel", "Back to Dashboard");
        } else {
            model.addAttribute("applicationDetails", applicationService.getApplicationById(id));
            model.addAttribute("backUrl", "/applications/my");
            model.addAttribute("backLabel", "Back to My Applications");
        }

        return "applications/details";
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#id)")
    public String acceptApplication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applicationService.acceptApplication(id);
        redirectAttributes.addFlashAttribute("successMessage", "Application accepted.");
        return "redirect:/applications/" + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#id)")
    public String rejectApplication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applicationService.rejectApplication(id);
        redirectAttributes.addFlashAttribute("successMessage", "Application rejected.");
        return "redirect:/applications/" + id;
    }
}
