package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.repository.CandidateRepository;
import com.narek.jobportal.service.CandidateService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateServiceImpl(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Override
    public Candidate saveCandidate(Candidate candidate) {
        return candidateRepository.save(candidate);
    }

    @Override
    public Optional<Candidate> getCandidateById(Long id) {
        return candidateRepository.findById(id);
    }

    @Override
    public Optional<Candidate> getCandidateByUserId(Long userId) {
        return candidateRepository.findByUserId(userId);
    }

    @Override
    public Optional<Candidate> getCandidateByUserEmail(String email) {
        return candidateRepository.findByUserEmail(email);
    }

    @Override
    @Transactional
    public Candidate updateOwnProfile(ProfileUpdateDto profileUpdateDto, String authenticatedEmail) {
        Candidate candidate = candidateRepository.findByUserEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("Candidate profile not found"));

        if (!candidate.getUser().getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        candidate.setFullName(profileUpdateDto.getFullName());
        candidate.setResumeUrl(profileUpdateDto.getResumeUrl());

        return candidateRepository.save(candidate);
    }
}
