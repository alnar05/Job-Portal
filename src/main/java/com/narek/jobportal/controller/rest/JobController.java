package com.narek.jobportal.controller.rest;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.entity.SearchSortOption;
import com.narek.jobportal.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<JobResponseDto> getAllJobs(@RequestParam(required = false) String query,
                                           @RequestParam(required = false) String location,
                                           @RequestParam(required = false) JobType jobType,
                                           @RequestParam(required = false) Double minSalary,
                                           @RequestParam(required = false) Double maxSalary,
                                           @RequestParam(required = false) String companyName,
                                           @RequestParam(defaultValue = "NEWEST") SearchSortOption sort) {
        return jobService.searchJobs(query, location, jobType, minSalary, maxSalary, companyName, sort, Pageable.unpaged()).getContent();
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public Page<JobResponseDto> searchJobs(@RequestParam(required = false) String query,
                                           @RequestParam(required = false) String location,
                                           @RequestParam(required = false) JobType jobType,
                                           @RequestParam(required = false) Double minSalary,
                                           @RequestParam(required = false) Double maxSalary,
                                           @RequestParam(required = false) String companyName,
                                           @RequestParam(defaultValue = "NEWEST") SearchSortOption sort,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jobService.searchJobs(query, location, jobType, minSalary, maxSalary, companyName, sort, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public JobResponseDto getJobById(@PathVariable Long id) {
        return jobService.getJobById(id);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/employer/{employerId}")
    public List<JobResponseDto> getJobsByEmployer(@PathVariable Long employerId) {
        return jobService.getJobsByEmployerId(employerId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public JobResponseDto createJob(@RequestBody @Valid JobCreateUpdateDto dto) {
        return jobService.createJob(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public JobResponseDto updateJob(@PathVariable Long id,
                                    @RequestBody @Valid JobCreateUpdateDto dto) {

        return jobService.updateJob(id, dto);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public String closeJob(@PathVariable Long id) {
        jobService.closeJob(id);
        return "Job closed";
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#id)")
    public String deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return "Job deleted successfully";
    }
}
