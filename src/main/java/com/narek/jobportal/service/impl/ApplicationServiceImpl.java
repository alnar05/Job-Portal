package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.AdminApplicationFilterDto;
import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.ApplicationStatus;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.exception.DuplicateApplicationException;
import com.narek.jobportal.exception.JobApplicationClosedException;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.specification.ApplicationSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final AuthService authService;

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
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + dto.getJobId()));

        Candidate candidate = authService.getCurrentCandidate();

        if (job.isExpired()) {
            throw new JobApplicationClosedException("Job application period has closed");
        }

        if (applicationRepository.existsByJobIdAndCandidateId(dto.getJobId(), candidate.getId())) {
            throw new DuplicateApplicationException(
                    "You have already already applied for job with id " + dto.getJobId()
            );
        }

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setCoverLetter(normalizeCoverLetter(dto.getCoverLetter()));

        Application saved = applicationRepository.save(application);

        return mapToResponseDto(saved);
    }

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
        return applicationRepository.findAll().stream().map(this::mapToResponseDto).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentCandidate(#candidateId)")
    public List<ApplicationResponseDto> getApplicationsByCandidateId(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId).stream().map(this::mapToResponseDto).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentEmployerJob(#jobId)")
    public List<ApplicationResponseDto> getApplicationsByJobId(Long jobId) {
        return applicationRepository.findByJobId(jobId).stream().map(this::mapToResponseDto).toList();
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

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ApplicationResponseDto cancelApplication(Long applicationId) {
        return updateFinalStatus(applicationId, ApplicationStatus.CANCELLED);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateStatus(Long id, ApplicationStatus status) {
        if (status == null) {
            return;
        }

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));

        application.setStatus(status);
        applicationRepository.save(application);
    }

    @Override
    @PreAuthorize("hasRole('CANDIDATE')")
    public boolean hasCandidateApplied(Long candidateId, Long jobId) {
        return applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ApplicationResponseDto> searchAdminApplications(AdminApplicationFilterDto filter, Pageable pageable) {
        Specification<Application> spec = Specification.where(ApplicationSpecification.byCandidate(filter.getCandidateId()))
                .and(ApplicationSpecification.byJob(filter.getJobId()))
                .and(ApplicationSpecification.byStatus(filter.getStatus()));
        return applicationRepository.findAll(spec, pageable).map(this::mapToResponseDto);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateStatusBulk(List<Long> applicationIds, ApplicationStatus status) {
        if (applicationIds == null || applicationIds.isEmpty() || status == null) return;
        List<Application> applications = applicationRepository.findAllById(applicationIds);
        applications.forEach(a -> a.setStatus(status));
        applicationRepository.saveAll(applications);
    }

    @Override
    public List<ApplicationResponseDto> getRecentApplications(int limit) {
        return applicationRepository.findTop5ByOrderByAppliedAtDesc().stream().limit(limit).map(this::mapToResponseDto).toList();
    }

    @Override
    public Map<Long, Long> countByJobIds(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) return Map.of();
        return applicationRepository.findAll().stream()
                .filter(a -> jobIds.contains(a.getJob().getId()))
                .collect(Collectors.groupingBy(a -> a.getJob().getId(), Collectors.counting()));
    }

    private ApplicationResponseDto updateFinalStatus(Long applicationId, ApplicationStatus targetStatus) {
        Application application = getManagedApplication(applicationId);
        if (application.getStatus() == ApplicationStatus.ACCEPTED || application.getStatus() == ApplicationStatus.REJECTED || application.getStatus() == ApplicationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application is already finalized");
        }
        application.setStatus(targetStatus);
        return mapToResponseDto(applicationRepository.save(application));
    }

    private String normalizeCoverLetter(String coverLetter) {
        if (coverLetter == null) return null;
        String trimmed = coverLetter.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Application getManagedApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private ApplicationResponseDto mapToResponseDto(Application app) {
        return new ApplicationResponseDto(app.getId(), app.getJob().getId(), app.getJob().getTitle(), app.getCandidate().getId(),
                app.getCandidate().getUser().getEmail(), app.getCoverLetter(), app.getAppliedAt(), app.getStatus());
    }
}
