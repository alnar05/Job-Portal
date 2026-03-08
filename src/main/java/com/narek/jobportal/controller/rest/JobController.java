package com.narek.jobportal.controller.rest;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public List<JobResponseDto> getAllJobs(@RequestParam(required = false) String query,
                                           @RequestParam(required = false) String location,
                                           @RequestParam(required = false) JobType jobType,
                                           @RequestParam(required = false) Double minSalary,
                                           @RequestParam(required = false) Double maxSalary) {
        if (query != null || location != null || jobType != null || minSalary != null || maxSalary != null) {
            return jobService.searchJobs(query, location, jobType, minSalary, maxSalary, Pageable.unpaged()).getContent();
        }
        return jobService.getAllJobs();
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public Page<JobResponseDto> searchJobs(@RequestParam(required = false) String query,
                                           @RequestParam(required = false) String location,
                                           @RequestParam(required = false) JobType jobType,
                                           @RequestParam(required = false) Double minSalary,
                                           @RequestParam(required = false) Double maxSalary,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(defaultValue = "id,desc") String[] sort) {
        Sort.Direction direction = sort.length > 1 && "asc".equalsIgnoreCase(sort[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        return jobService.searchJobs(query, location, jobType, minSalary, maxSalary, pageable);
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
