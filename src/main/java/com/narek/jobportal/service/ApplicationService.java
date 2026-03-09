package com.narek.jobportal.service;

import com.narek.jobportal.dto.AdminApplicationFilterDto;
import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

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

    ApplicationResponseDto cancelApplication(Long applicationId);

    Page<ApplicationResponseDto> searchAdminApplications(AdminApplicationFilterDto filter, Pageable pageable);

    void updateStatusBulk(List<Long> applicationIds, ApplicationStatus status);

    List<ApplicationResponseDto> getRecentApplications(int limit);

    Map<Long, Long> countByJobIds(List<Long> jobIds);
}
