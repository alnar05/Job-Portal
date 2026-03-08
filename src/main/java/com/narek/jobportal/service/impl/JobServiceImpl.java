package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
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
    public List<JobResponseDto> getAllJobs() {
        logger.info("Fetching all active jobs");
        return jobRepository.findAll(JobSpecification.notExpired())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public JobResponseDto getJobById(Long id) {
        logger.info("Fetching job by id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));

        return mapToResponse(job);
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

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    @Override
    public JobResponseDto updateJob(Long id, JobCreateUpdateDto dto) {
        logger.info("Updating job id={}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + id));

        applyDto(job, dto);

        Job updatedJob = jobRepository.save(job);
        return mapToResponse(updatedJob);
    }

    @Override
    public Page<JobResponseDto> searchJobs(String keyword,
                                           String location,
                                           JobType jobType,
                                           Double minSalary,
                                           Double maxSalary,
                                           Pageable pageable) {
        logger.info("Searching jobs keyword={}, location={}, jobType={}, minSalary={}, maxSalary={}, page={}",
                keyword, location, jobType, minSalary, maxSalary, pageable.getPageNumber());

        Specification<Job> spec = Specification.where(JobSpecification.notExpired())
                .and(JobSpecification.hasKeyword(keyword))
                .and(JobSpecification.hasLocation(location))
                .and(JobSpecification.hasJobType(jobType))
                .and(JobSpecification.overlapsSalaryRange(minSalary, maxSalary));

        return jobRepository.findAll(spec, pageable).map(this::mapToResponse);
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
        return dto;
    }

}
