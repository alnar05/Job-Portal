package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
        application.setCoverLetter(dto.getCoverLetter());
        application.setAppliedAt(LocalDateTime.now());

        Application saved = applicationRepository.save(application);

        return mapToResponseDto(saved);
    }

    // Disabled update method
    @Override
    public ApplicationResponseDto updateApplication(Long id, ApplicationCreateUpdateDto dto) {
        throw new UnsupportedOperationException("Updating applications is not allowed");
    }

    @Override
    public void deleteApplication(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        applicationRepository.delete(application);
    }

    @Override
    public ApplicationResponseDto getApplicationById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        return mapToResponseDto(application);
    }

    @Override
    public List<ApplicationResponseDto> getAllApplications() {
        return applicationRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<ApplicationResponseDto> getApplicationsByCandidateId(Long candidateId) {
        return applicationRepository.findByCandidateId(candidateId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<ApplicationResponseDto> getApplicationsByJobId(Long jobId) {
        return applicationRepository.findByJobId(jobId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    private ApplicationResponseDto mapToResponseDto(Application app) {
        return new ApplicationResponseDto(
                app.getId(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getCandidate().getId(),
                app.getCandidate().getUser().getEmail(),
                app.getCoverLetter(),
                app.getAppliedAt()
        );
    }
}
