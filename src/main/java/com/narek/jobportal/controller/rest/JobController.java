package com.narek.jobportal.controller.rest;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.service.JobService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // Get all jobs
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<JobResponseDto> getAllJobs() {
        return jobService.getAllJobs();
    }

    // Get job by ID
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public JobResponseDto getJobById(@PathVariable Long id) {
        return jobService.getJobById(id);
    }

    // Get jobs by employer
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/employer/{employerId}")
    public List<JobResponseDto> getJobsByEmployer(@PathVariable Long employerId) {
        return jobService.getJobsByEmployerId(employerId);
    }

    // Create job
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public JobResponseDto createJob(@RequestBody @Valid JobCreateUpdateDto dto) {
        return jobService.createJob(dto);
    }

    // Update job
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public JobResponseDto updateJob(@PathVariable Long id,
                                    @RequestBody @Valid JobCreateUpdateDto dto) {

        return jobService.updateJob(id, dto);
    }

    // Delete job
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public String deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return "Job deleted successfully";
    }
}
