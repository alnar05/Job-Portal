package com.narek.jobportal.controller.rest;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // Get all applications
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<ApplicationResponseDto> getAllApplications() {
        return applicationService.getAllApplications();
    }

    // Get application by ID (employer, admin or the candidate)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN') or @authService.isCurrentCandidateApplication(#id)")
    public ApplicationResponseDto getApplicationById(@PathVariable Long id) {
        return applicationService.getApplicationById(id);
    }

    // Create application (candidate only)
    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationResponseDto createApplication(
            @RequestBody @Valid ApplicationCreateUpdateDto dto) {
        return applicationService.createApplication(dto);
    }

    // Get applications by candidate
    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<ApplicationResponseDto> getApplicationsByCandidate(@PathVariable Long candidateId) {
        return applicationService.getApplicationsByCandidateId(candidateId);
    }

    // Get applications by job
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<ApplicationResponseDto> getApplicationsByJob(@PathVariable Long jobId) {
        return applicationService.getApplicationsByJobId(jobId);
    }

    // Delete application (admin or the candidate)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentCandidateApplication(#id)")
    public String deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return "Application deleted successfully";
    }
}
