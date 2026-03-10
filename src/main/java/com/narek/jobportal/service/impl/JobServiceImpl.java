package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.AdminJobFilterDto;
import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.JobStatus;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.specification.AdminJobSpecification;
import com.narek.jobportal.specification.JobSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    @Transactional
    public List<JobResponseDto> getAllJobs() {
        logger.info("Fetching all active jobs");
        return jobRepository.findAll(JobSpecification.notExpired())
                .stream()
                .peek(this::normalizeStatus)
                .filter(job -> job.getStatus() == JobStatus.ACTIVE)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public JobResponseDto getJobById(Long id) {
        logger.info("Fetching job by id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));

        normalizeStatus(job);
        return mapToResponse(job);
    }

    @Override
    @Transactional
    public List<JobResponseDto> getJobsByEmployerId(Long employerId) {
        logger.info("Fetching jobs for employerId={}", employerId);
        return jobRepository.findByEmployerId(employerId)
                .stream()
                .peek(this::normalizeStatus)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        logger.info("Deleting job id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));
        applicationRepository.deleteByJobId(job.getId());
        jobRepository.delete(job);
    }

    @Override
    public JobResponseDto createJob(JobCreateUpdateDto dto) {
        logger.info("Creating job with title={}", dto.getTitle());
        Employer employer = authService.getCurrentEmployer();

        Job job = new Job();
        applyDto(job, dto);
        job.setEmployer(employer);
        job.setStatus(JobStatus.ACTIVE);

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    @Override
    @Transactional
    public JobResponseDto updateJob(Long id, JobCreateUpdateDto dto) {
        logger.info("Updating job id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));

        normalizeStatus(job);
        if (job.getStatus() == JobStatus.EXPIRED) {
            throw new IllegalArgumentException("Expired jobs cannot be modified. Duplicate the job to repost.");
        }

        applyDto(job, dto);
        normalizeStatus(job);

        Job updatedJob = jobRepository.save(job);
        return mapToResponse(updatedJob);
    }

    @Override
    @Transactional
    public Page<JobResponseDto> searchJobs(String keyword,
                                           String location,
                                           JobType jobType,
                                           Double minSalary,
                                           Double maxSalary,
                                           Pageable pageable) {
        Specification<Job> spec = Specification.where(JobSpecification.notExpired())
                .and(JobSpecification.hasKeyword(keyword))
                .and(JobSpecification.hasLocation(location))
                .and(JobSpecification.hasJobType(jobType))
                .and(JobSpecification.overlapsSalaryRange(minSalary, maxSalary))
                .and(AdminJobSpecification.hasStatus(JobStatus.ACTIVE));

        Page<Job> jobs = jobRepository.findAll(spec, pageable);
        jobs.forEach(this::normalizeStatus);
        return jobs.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public Page<Job> searchAdminJobs(AdminJobFilterDto filter, Pageable pageable) {
        if (filter.getStatus() == JobStatus.EXPIRED) {
            Specification<Job> spec = Specification.where(AdminJobSpecification.expiredOnly())
                    .and(AdminJobSpecification.minSalary(filter.getMinSalary()))
                    .and(AdminJobSpecification.maxSalary(filter.getMaxSalary()))
                    .and(AdminJobSpecification.byEmployer(filter.getEmployerId()));
            return jobRepository.findAll(spec, pageable).map(this::normalizeStatusAndReturn);
        }

        Specification<Job> spec = Specification.where(AdminJobSpecification.hasStatus(filter.getStatus()))
                .and(AdminJobSpecification.minSalary(filter.getMinSalary()))
                .and(AdminJobSpecification.maxSalary(filter.getMaxSalary()))
                .and(AdminJobSpecification.byEmployer(filter.getEmployerId()));

        Page<Job> jobs = jobRepository.findAll(spec, pageable);
        jobs.forEach(this::normalizeStatus);
        return jobs;
    }

    @Override
    @Transactional
    public void updateStatusBulk(List<Long> jobIds, boolean reopen) {
        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }
        List<Job> jobs = jobRepository.findAllById(jobIds);
        jobs.forEach(this::normalizeStatus);

        List<Job> mutableJobs = jobs.stream()
                .filter(job -> job.getStatus() != JobStatus.EXPIRED)
                .toList();

        mutableJobs.forEach(job -> job.setStatus(reopen ? JobStatus.ACTIVE : JobStatus.CLOSED));
        jobRepository.saveAll(mutableJobs);
    }

    @Override
    public List<Job> getRecentJobs(int limit) {
        return jobRepository.findTop5ByOrderByCreatedAtDesc().stream().limit(limit).toList();
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
        dto.setStatus(job.getEffectiveStatus());
        return dto;
    }

    private void normalizeStatus(Job job) {
        if (job.isExpired() && job.getStatus() != JobStatus.EXPIRED) {
            job.setStatus(JobStatus.EXPIRED);
            jobRepository.save(job);
        }
    }

    private Job normalizeStatusAndReturn(Job job) {
        normalizeStatus(job);
        return job;
    }
}
