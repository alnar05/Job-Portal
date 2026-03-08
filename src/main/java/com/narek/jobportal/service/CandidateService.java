package com.narek.jobportal.service;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Candidate;
import java.util.Optional;

public interface CandidateService {
    Candidate saveCandidate(Candidate candidate);
    Optional<Candidate> getCandidateById(Long id);
    Optional<Candidate> getCandidateByUserId(Long userId);
    Optional<Candidate> getCandidateByUserEmail(String email);
    Candidate updateOwnProfile(ProfileUpdateDto profileUpdateDto, String authenticatedEmail);
}
