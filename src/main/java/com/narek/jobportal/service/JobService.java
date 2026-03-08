package com.narek.jobportal.service;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.dto.SavedSearchDto;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.entity.SearchSortOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobService {
    List<JobResponseDto> getAllJobs();
    JobResponseDto getJobById(Long id);
    List<JobResponseDto> getJobsByEmployerId(Long employerId);
    void deleteJob(Long id);
    JobResponseDto createJob(JobCreateUpdateDto dto);
    JobResponseDto updateJob(Long id, JobCreateUpdateDto dto);
    void closeJob(Long id);

    Page<JobResponseDto> searchJobs(
            String keyword,
            String location,
            JobType jobType,
            Double minSalary,
            Double maxSalary,
            String companyName,
            SearchSortOption sortOption,
            Pageable pageable
    );

    SavedSearchDto saveSearch(SavedSearchDto savedSearchDto);

    List<SavedSearchDto> getSavedSearchesForCurrentCandidate();

    void expireOverdueJobs();
}
