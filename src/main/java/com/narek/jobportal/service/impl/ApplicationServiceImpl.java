package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.ApplicationStatus;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final AuthService authService; // for getting logged-in candidate

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  JobRepository jobRepository,
                                  AuthService authService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public ApplicationResponseDto createApplication(ApplicationCreateUpdateDto dto) {
        Job job = jobRepository.findById(dto.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Candidate candidate = authService.getCurrentCandidate();

        // prevent duplicate applications
        if (applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already applied to this job");
        }

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setCoverLetter(normalizeCoverLetter(dto.getCoverLetter()));

        Application saved = applicationRepository.save(application);

        return mapToResponseDto(saved);
    }

    // Disabled update method
    @Override
    public ApplicationResponseDto updateApplication(Long id, ApplicationCreateUpdateDto dto) {
        throw new UnsupportedOperationException("Updating applications is not allowed");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteApplication(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        applicationRepository.delete(application);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#id) or @authService.isCurrentCandidateApplication(#id)")
    public ApplicationResponseDto getApplicationById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        return mapToResponseDto(application);
    }

    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public List<ApplicationResponseDto> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentCandidate(#candidateId)")
    public List<ApplicationResponseDto> getApplicationsByCandidateId(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#jobId)")
    public List<ApplicationResponseDto> getApplicationsByJobId(Long jobId) {
        return applicationRepository.findByJobId(jobId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#applicationId)")
    public ApplicationResponseDto markAsReviewed(Long applicationId) {
        Application application = getManagedApplication(applicationId);
        if (application.getStatus() == ApplicationStatus.APPLIED) {
            application.setStatus(ApplicationStatus.REVIEWED);
        }
        return mapToResponseDto(applicationRepository.save(application));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#applicationId)")
    public ApplicationResponseDto acceptApplication(Long applicationId) {
        return updateFinalStatus(applicationId, ApplicationStatus.ACCEPTED);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerApplication(#applicationId)")
    public ApplicationResponseDto rejectApplication(Long applicationId) {
        return updateFinalStatus(applicationId, ApplicationStatus.REJECTED);
    }

    private ApplicationResponseDto updateFinalStatus(Long applicationId, ApplicationStatus targetStatus) {
        Application application = getManagedApplication(applicationId);
        if (application.getStatus() == ApplicationStatus.ACCEPTED || application.getStatus() == ApplicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application is already finalized");
        }

        application.setStatus(targetStatus);
        return mapToResponseDto(applicationRepository.save(application));
    }

    private String normalizeCoverLetter(String coverLetter) {
        if (coverLetter == null) {
            return null;
        }

        String trimmed = coverLetter.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Application getManagedApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private ApplicationResponseDto mapToResponseDto(Application app) {
        return new ApplicationResponseDto(
                app.getId(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getCandidate().getId(),
                app.getCandidate().getUser().getEmail(),
                app.getCoverLetter(),
                app.getAppliedAt(),
                app.getStatus()
        );
    }
}
