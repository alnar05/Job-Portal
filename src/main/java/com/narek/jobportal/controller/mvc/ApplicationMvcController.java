package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/applications")
public class ApplicationMvcController {

    private final ApplicationService applicationService;
    private final AuthService authService;
    private final JobService jobService;

    public ApplicationMvcController(ApplicationService applicationService,
                                    AuthService authService,
                                    JobService jobService) {
        this.applicationService = applicationService;
        this.authService = authService;
        this.jobService = jobService;
    }

    @PostMapping("/apply/{jobId}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public String applyForJob(@PathVariable Long jobId,
                              @RequestParam(name = "coverLetter", required = false) String coverLetter,
                              RedirectAttributes redirectAttributes) {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto();
        dto.setJobId(jobId);
        dto.setCoverLetter(coverLetter);

        try {
            applicationService.createApplication(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Application submitted successfully.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }

        return "redirect:/jobs/" + jobId;
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
}