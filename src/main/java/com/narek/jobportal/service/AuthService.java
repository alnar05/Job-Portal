package com.narek.jobportal.service;

import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;

public interface AuthService {
    Employer getCurrentEmployer();

    Candidate getCurrentCandidate();
}
