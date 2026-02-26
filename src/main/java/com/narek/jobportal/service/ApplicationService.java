package com.narek.jobportal.service;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import java.util.List;

public interface ApplicationService {

    ApplicationResponseDto createApplication(ApplicationCreateUpdateDto dto);

    ApplicationResponseDto updateApplication(Long id, ApplicationCreateUpdateDto dto);

    void deleteApplication(Long id);

    ApplicationResponseDto getApplicationById(Long id);

    List<ApplicationResponseDto> getAllApplications();

    List<ApplicationResponseDto> getApplicationsByCandidateId(Long candidateId);

    List<ApplicationResponseDto> getApplicationsByJobId(Long jobId);

    ApplicationResponseDto markAsReviewed(Long applicationId);

    ApplicationResponseDto acceptApplication(Long applicationId);

    ApplicationResponseDto rejectApplication(Long applicationId);
}
