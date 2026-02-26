package com.narek.jobportal.service;

import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;

public interface AuthService {
    Employer getCurrentEmployer();

    Candidate getCurrentCandidate();

    boolean isCurrentCandidateApplication(Long applicationId);

    boolean isCurrentEmployerJob(Long jobId);

    boolean isCurrentEmployerApplication(Long applicationId);
}
