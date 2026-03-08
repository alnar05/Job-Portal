package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.entity.*;
import com.narek.jobportal.exception.DuplicateApplicationException;
import com.narek.jobportal.exception.JobApplicationClosedException;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final AuthService authService;
    private final NotificationService notificationService;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  JobRepository jobRepository,
                                  AuthService authService,
                                  NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ApplicationResponseDto createApplication(ApplicationCreateUpdateDto dto) {
        Job job = jobRepository.findById(dto.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + dto.getJobId()));

        Candidate candidate = authService.getCurrentCandidate();

        if (job.isExpired() || job.getStatus() != JobStatus.OPEN) {
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
        application.setCoverLetter(normalize(dto.getCoverLetter()));

        Application saved = applicationRepository.save(application);
        notificationService.notify(job.getEmployer().getUser(), "New application", "A new application was received for " + job.getTitle());

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
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYER')")
    public void bulkUpdateStatus(List<Long> ids, boolean accept, String internalNotes) {
        for (Long id : ids) {
            Application app = getManagedApplication(id);
            app.setStatus(accept ? ApplicationStatus.ACCEPTED : ApplicationStatus.REJECTED);
            if (internalNotes != null && !internalNotes.isBlank()) {
                app.setInternalNotes(internalNotes.trim());
            }
            notificationService.notify(app.getCandidate().getUser(), "Application update",
                    "Your application for '" + app.getJob().getTitle() + "' is now " + app.getStatus());
        }
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYER')")
    public ApplicationResponseDto addInternalNotes(Long applicationId, String internalNotes) {
        Application app = getManagedApplication(applicationId);
        app.setInternalNotes(normalize(internalNotes));
        return mapToResponseDto(applicationRepository.save(app));
    }

    @Override
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void autoRejectExpiredJobApplications() {
        List<Job> jobs = jobRepository.findAll();
        for (Job job : jobs) {
            if (!job.isExpired()) {
                continue;
            }
            List<Application> activeApps = applicationRepository.findByJobIdAndStatusIn(job.getId(),
                    List.of(ApplicationStatus.APPLIED, ApplicationStatus.REVIEWED));
            for (Application app : activeApps) {
                app.setStatus(ApplicationStatus.AUTO_REJECTED);
                notificationService.notify(app.getCandidate().getUser(), "Application auto-rejected",
                        "Application for '" + app.getJob().getTitle() + "' was auto-rejected after closing date.");
            }
        }
    }

    private ApplicationResponseDto updateFinalStatus(Long applicationId, ApplicationStatus finalStatus) {
        Application application = getManagedApplication(applicationId);
        application.setStatus(finalStatus);
        Application saved = applicationRepository.save(application);
        notificationService.notify(saved.getCandidate().getUser(), "Application status changed",
                "Your application for '" + saved.getJob().getTitle() + "' is now " + saved.getStatus());
        return mapToResponseDto(saved);
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
                app.getCandidate().getFullName(),
                app.getCoverLetter(),
                app.getInternalNotes(),
                app.getAppliedAt(),
                app.getStatus()
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
