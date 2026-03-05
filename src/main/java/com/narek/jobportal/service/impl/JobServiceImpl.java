package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final AuthService authService;

    public JobServiceImpl(JobRepository jobRepository,
                          ApplicationRepository applicationRepository,
                          AuthService authService) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.authService = authService;
    }

    @Override
    public List<JobResponseDto> getAllJobs() {
        logger.info("Fetching all jobs without pagination");
        List<JobResponseDto> jobs = jobRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
        logger.info("Fetched {} jobs without pagination", jobs.size());
        return jobs;
    }

    @Override
    public Page<JobResponseDto> getAllJobs(Pageable pageable) {
        logger.info("Fetching jobs with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        Page<JobResponseDto> page = jobRepository.findAll(pageable)
                .map(this::mapToResponse);
        logger.info("Fetched paged jobs: page={}, totalElements={}, totalPages={}",
                page.getNumber(), page.getTotalElements(), page.getTotalPages());
        return page;
    }

    @Override
    public Page<JobResponseDto> searchJobs(String keyword, Pageable pageable) {
        logger.info("Searching jobs by keyword='{}' with pagination: page={}, size={}, sort={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<JobResponseDto> page = jobRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable)
                .map(this::mapToResponse);

        logger.info("Search completed for keyword='{}': found {} jobs", keyword, page.getTotalElements());
        return page;
    }

    @Override
    public JobResponseDto getJobById(Long id) {
        logger.info("Fetching job by id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));

        logger.info("Job found for id={}", id);
        return mapToResponse(job);
    }

    @Override
    public List<JobResponseDto> getJobsByEmployerId(Long employerId) {
        logger.info("Fetching jobs by employerId={}", employerId);
        List<JobResponseDto> jobs = jobRepository.findByEmployerId(employerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        logger.info("Fetched {} jobs for employerId={}", jobs.size(), employerId);
        return jobs;
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        logger.info("Deleting job id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));
        applicationRepository.deleteByJobId(job.getId());
        jobRepository.delete(job);
        logger.info("Deleted job id={} and its related applications", id);
    }

    @Override
    public JobResponseDto createJob(JobCreateUpdateDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Job request cannot be null");
        }
        logger.info("Creating new job with title='{}'", dto.getTitle());
        Employer employer = authService.getCurrentEmployer();

        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setSalary(dto.getSalary());
        job.setEmployer(employer);

        Job savedJob = jobRepository.save(job);
        logger.info("Created job id={} for employerId={}", savedJob.getId(), employer.getId());
        return mapToResponse(savedJob);
    }

    @Override
    public JobResponseDto updateJob(Long id, JobCreateUpdateDto dto) {
        logger.info("Updating job id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));

        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setSalary(dto.getSalary());

        Job updatedJob = jobRepository.save(job);
        logger.info("Updated job id={}", id);
        return mapToResponse(updatedJob);
    }

    private JobResponseDto mapToResponse(Job job) {
        JobResponseDto dto = new JobResponseDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setSalary(job.getSalary());
        dto.setCompanyName(job.getEmployer().getCompanyName());
        return dto;
    }

}
