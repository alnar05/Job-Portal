package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.*;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.service.AuthService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("authService")
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public AuthServiceImpl(UserRepository userRepository, JobRepository jobRepository, ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public Employer getCurrentEmployer() {

        User user = getCurrentUser()
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));

        if (user.getEmployer() == null) {
            throw new AccessDeniedException("User is not an employer");
        }

        return user.getEmployer();
    }

    @Override
    public Candidate getCurrentCandidate() {
        User user = getCurrentUser()
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));

        if (user.getCandidate() == null) {
            throw new AccessDeniedException("User is not a candidate");
        }

        return user.getCandidate();
    }

    @Override
    public boolean isCurrentCandidate(Long candidateId) {
        return getCurrentUser()
                .map(User::getCandidate)
                .filter(candidate -> candidate != null && candidate.getId() != null)
                .map(candidate -> candidate.getId().equals(candidateId))
                .orElse(false);
    }

    // Check if the currently logged-in candidate owns the application
    @Override
    public boolean isCurrentCandidateApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .map(app -> getCurrentUser()
                        .map(User::getCandidate)
                        .map(Candidate::getId)
                        .filter(candidateId -> candidateId.equals(app.getCandidate().getId()))
                        .isPresent())
                .orElse(false);
    }

    // Check if the currently logged-in employer owns the job
    @Override
    public boolean isCurrentEmployerJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id " + jobId));
        return getCurrentUser()
                .map(User::getEmployer)
                .map(Employer::getId)
                .filter(employerId -> employerId.equals(job.getEmployer().getId()))
                .isPresent();
    }

    @Override
    public boolean isCurrentEmployerApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .map(app -> getCurrentUser()
                        .map(User::getEmployer)
                        .map(Employer::getId)
                        .filter(employerId -> employerId.equals(app.getJob().getEmployer().getId()))
                        .isPresent())
                .orElse(false);
    }

    private Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email);
    }
}
