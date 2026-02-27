package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobServiceImpl implements JobService {

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
        return jobRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public JobResponseDto getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        return mapToResponse(job);
    }

    @Override
    public List<JobResponseDto> getJobsByEmployerId(Long employerId) {
        return jobRepository.findByEmployerId(employerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        applicationRepository.deleteByJobId(job.getId());
        jobRepository.delete(job);
    }

    @Override
    public JobResponseDto createJob(JobCreateUpdateDto dto) {
        Employer employer = authService.getCurrentEmployer();

        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setSalary(dto.getSalary());
        job.setEmployer(employer);

        Job savedJob = jobRepository.save(job);
        return mapToResponse(savedJob);
    }

    @Override
    public JobResponseDto updateJob(Long id, JobCreateUpdateDto dto) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setSalary(dto.getSalary());

        Job updatedJob = jobRepository.save(job);
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
