package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.*;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.service.NotificationService;
import com.narek.jobportal.specification.JobSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final AuthService authService;
    private final NotificationService notificationService;

    public JobServiceImpl(JobRepository jobRepository,
                          ApplicationRepository applicationRepository,
                          AuthService authService,
                          NotificationService notificationService) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Override
    public List<JobResponseDto> getAllJobs() {
        logger.info("Fetching all active jobs");
        return jobRepository.findAll(JobSpecification.publiclyVisible())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<JobResponseDto> getAllJobsForManagement() {
        return jobRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public JobResponseDto getJobById(Long id) {
        logger.info("Fetching job by id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));
        job.setViewCount(job.getViewCount() + 1);
        return mapToResponse(jobRepository.save(job));
    }

    @Override
    public List<JobResponseDto> getJobsByEmployerId(Long employerId) {
        logger.info("Fetching jobs for employerId={}", employerId);
        return jobRepository.findByEmployerId(employerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));
        applicationRepository.deleteByJobId(job.getId());
        jobRepository.delete(job);
    }

    @Override
    public JobResponseDto createJob(JobCreateUpdateDto dto) {
        Employer employer = authService.getCurrentEmployer();
        if (employer.getUser().getStatus() == UserStatus.BANNED) {
            throw new org.springframework.security.access.AccessDeniedException("Banned employers cannot post jobs");
        }

        Job job = new Job();
        applyDto(job, dto);
        job.setEmployer(employer);

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    @Override
    public JobResponseDto updateJob(Long id, JobCreateUpdateDto dto) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));

        applyDto(job, dto);

        Job updatedJob = jobRepository.save(job);
        return mapToResponse(updatedJob);
    }

    @Override
    public void closeJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);
    }

    @Override
    public Page<JobResponseDto> searchJobs(String keyword,
                                           String location,
                                           JobType jobType,
                                           Double minSalary,
                                           Double maxSalary,
                                           Pageable pageable) {
        return searchJobs(keyword, location, jobType, minSalary, maxSalary, null, pageable);
    }

    @Override
    public Page<JobResponseDto> searchJobs(String keyword,
                                           String location,
                                           JobType jobType,
                                           Double minSalary,
                                           Double maxSalary,
                                           String companyName,
                                           Pageable pageable) {
        return searchJobs(keyword, location, jobType, minSalary, maxSalary, companyName, SearchSortOption.NEWEST, pageable);
    }

    @Override
    public Page<JobResponseDto> searchJobs(String keyword,
                                           String location,
                                           JobType jobType,
                                           Double minSalary,
                                           Double maxSalary,
                                           String companyName,
                                           SearchSortOption sortOption,
                                           Pageable pageable) {

        Specification<Job> spec = Specification.where(JobSpecification.notExpired())
                .and(JobSpecification.isOpen())
                .and(JobSpecification.hasKeyword(keyword))
                .and(JobSpecification.hasLocation(location))
                .and(JobSpecification.hasJobType(jobType))
                .and(JobSpecification.overlapsSalaryRange(minSalary, maxSalary))
                .and(JobSpecification.hasCompanyName(companyName));

        Pageable effective = pageable;
        if (sortOption != null) {
            Sort sort = switch (sortOption) {
                case HIGHEST_SALARY -> Sort.by(Sort.Direction.DESC, "salary");
                case CLOSING_DATE -> Sort.by(Sort.Direction.ASC, "closingDate");
                default -> Sort.by(Sort.Direction.DESC, "createdAt");
            };
            int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
            int pageSize = pageable.isPaged() ? pageable.getPageSize() : 1000;
            effective = org.springframework.data.domain.PageRequest.of(pageNumber, pageSize, sort);
        }

        return jobRepository.findAll(spec, effective).map(this::mapToResponse);
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOverdueJobs() {
        List<Job> overdue = jobRepository.findByStatusAndClosingDateBefore(JobStatus.OPEN, LocalDate.now());
        for (Job job : overdue) {
            job.setStatus(JobStatus.EXPIRED);
            notificationService.notify(job.getEmployer().getUser(), "Job expired", "Job '" + job.getTitle() + "' has expired.");
        }
    }

    private void applyDto(Job job, JobCreateUpdateDto dto) {
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setSalary(dto.getSalary());
        job.setJobType(dto.getJobType());
        job.setLocation(dto.getLocation());
        job.setClosingDate(dto.getClosingDate());
    }

    private JobResponseDto mapToResponse(Job job) {
        JobResponseDto dto = new JobResponseDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setSalary(job.getSalary());
        dto.setJobType(job.getJobType());
        dto.setLocation(job.getLocation());
        dto.setClosingDate(job.getClosingDate());
        dto.setCompanyName(job.getEmployer().getCompanyName());
        dto.setStatus(job.getStatus());
        dto.setViewCount(job.getViewCount());
        dto.setApplicationCount(applicationRepository.countByJobId(job.getId()));
        return dto;
    }

}
