package com.narek.jobportal.controller.rest;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.dto.BulkApplicationActionDto;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.CandidateService;
import com.narek.jobportal.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final AuthService authService;
    private final ResumeService resumeService;
    private final CandidateService candidateService;

    public ApplicationController(ApplicationService applicationService, AuthService authService, ResumeService resumeService, CandidateService candidateService) {
        this.applicationService = applicationService;
        this.authService = authService;
        this.resumeService = resumeService;
        this.candidateService = candidateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<ApplicationResponseDto> getAllApplications() {
        return applicationService.getAllApplications();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN') or @authService.isCurrentCandidateApplication(#id)")
    public ApplicationResponseDto getApplicationById(@PathVariable Long id) {
        return applicationService.getApplicationById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationResponseDto createApplication(@RequestBody @Valid ApplicationCreateUpdateDto dto) {
        return applicationService.createApplication(dto);
    }

    @PostMapping(value = "/with-resume", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationResponseDto createWithResume(@RequestPart("payload") @Valid ApplicationCreateUpdateDto dto,
                                                   @RequestPart("resumeFile") MultipartFile resumeFile) {
        Candidate candidate = authService.getCurrentCandidate();
        candidate.setResumeFilePath(resumeService.storeResume(resumeFile));
        candidate.setResumeFileName(resumeFile.getOriginalFilename());
        candidate.setParsedResumeSummary(resumeService.parseResumeSummary(resumeFile));
        candidateService.saveCandidate(candidate);
        return applicationService.createApplication(dto);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public String bulkAction(@RequestBody @Valid BulkApplicationActionDto dto) {
        applicationService.bulkUpdateStatus(dto.getApplicationIds(), dto.getAccept(), dto.getInternalNotes());
        return "Bulk action completed";
    }

    @PutMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public ApplicationResponseDto addNotes(@PathVariable Long id, @RequestParam String notes) {
        return applicationService.addInternalNotes(id, notes);
    }

    @GetMapping("/candidate/{candidateId}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<ApplicationResponseDto> getApplicationsByCandidate(@PathVariable Long candidateId) {
        return applicationService.getApplicationsByCandidateId(candidateId);
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<ApplicationResponseDto> getApplicationsByJob(@PathVariable Long jobId) {
        return applicationService.getApplicationsByJobId(jobId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return "Application deleted successfully";
    }
}
