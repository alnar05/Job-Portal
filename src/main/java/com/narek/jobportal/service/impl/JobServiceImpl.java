package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.dto.SavedSearchDto;
import com.narek.jobportal.entity.*;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.repository.SavedSearchRepository;
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
    private final SavedSearchRepository savedSearchRepository;
    private final AuthService authService;
    private final NotificationService notificationService;

    public JobServiceImpl(JobRepository jobRepository,
                          ApplicationRepository applicationRepository,
                          SavedSearchRepository savedSearchRepository,
                          AuthService authService,
                          NotificationService notificationService) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Override
    public List<JobResponseDto> getAllJobs() {
        logger.info("Fetching all active jobs");
        return jobRepository.findAll(JobSpecification.notExpired())
                .stream()
                .map(this::mapToResponse)
                .toList();
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
                                           String companyName,
                                           SearchSortOption sortOption,
                                           Pageable pageable) {

        Specification<Job> spec = Specification.where(JobSpecification.notExpired())
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
    @Transactional
    public SavedSearchDto saveSearch(SavedSearchDto dto) {
        Candidate candidate = authService.getCurrentCandidate();
        SavedSearch savedSearch = new SavedSearch();
        savedSearch.setCandidate(candidate);
        savedSearch.setName(dto.getName());
        savedSearch.setKeyword(dto.getKeyword());
        savedSearch.setLocation(dto.getLocation());
        savedSearch.setCompanyName(dto.getCompanyName());
        savedSearch.setJobType(dto.getJobType());
        savedSearch.setMinSalary(dto.getMinSalary());
        savedSearch.setMaxSalary(dto.getMaxSalary());
        savedSearch.setSortOption(dto.getSortOption() == null ? SearchSortOption.NEWEST : dto.getSortOption());
        SavedSearch saved = savedSearchRepository.save(savedSearch);
        dto.setId(saved.getId());
        return dto;
    }

    @Override
    public List<SavedSearchDto> getSavedSearchesForCurrentCandidate() {
        Candidate candidate = authService.getCurrentCandidate();
        return savedSearchRepository.findByCandidateIdOrderByCreatedAtDesc(candidate.getId()).stream().map(saved -> {
            SavedSearchDto dto = new SavedSearchDto();
            dto.setId(saved.getId());
            dto.setName(saved.getName());
            dto.setKeyword(saved.getKeyword());
            dto.setLocation(saved.getLocation());
            dto.setCompanyName(saved.getCompanyName());
            dto.setJobType(saved.getJobType());
            dto.setMinSalary(saved.getMinSalary());
            dto.setMaxSalary(saved.getMaxSalary());
            dto.setSortOption(saved.getSortOption());
            return dto;
        }).toList();
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
