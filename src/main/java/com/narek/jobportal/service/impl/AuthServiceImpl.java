package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.*;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        if (user.getEmployer() == null) {
            throw new RuntimeException("User is not an employer");
        }

        return user.getEmployer();
    }

    @Override
    public Candidate getCurrentCandidate() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        if (user.getCandidate() == null) {
            throw new RuntimeException("User is not a candidate");
        }

        return user.getCandidate();
    }

    // Check if the currently logged-in candidate owns the application
    @Override
    public boolean isCurrentCandidateApplication(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        return getCurrentCandidate().getId().equals(app.getCandidate().getId());
    }
    
    // Check if the currently logged-in employer owns the job
    @Override
    public boolean isCurrentEmployerJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        return getCurrentEmployer().getId().equals(job.getEmployer().getId());
    }

    @Override
    public boolean isCurrentEmployerApplication(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        return getCurrentEmployer().getId().equals(app.getJob().getEmployer().getId());
    }

}
