package com.narek.jobportal.service;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobService {
    List<JobResponseDto> getAllJobs();
    Page<JobResponseDto> getAllJobs(Pageable pageable);
    Page<JobResponseDto> searchJobs(String keyword, Pageable pageable);
    JobResponseDto getJobById(Long id);
    List<JobResponseDto> getJobsByEmployerId(Long employerId);
    void deleteJob(Long id);
    JobResponseDto createJob(JobCreateUpdateDto dto);
    JobResponseDto updateJob(Long id, JobCreateUpdateDto dto);
}
