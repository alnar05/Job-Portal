package com.narek.jobportal.service;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import java.util.List;

public interface JobService {
    List<JobResponseDto> getAllJobs();
    JobResponseDto getJobById(Long id);
    List<JobResponseDto> getJobsByEmployerId(Long employerId);
    void deleteJob(Long id);
    JobResponseDto createJob(JobCreateUpdateDto dto);
    JobResponseDto updateJob(Long id, JobCreateUpdateDto dto);
}
