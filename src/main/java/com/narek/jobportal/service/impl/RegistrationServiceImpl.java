package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.RegistrationDto;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.CandidateRepository;
import com.narek.jobportal.repository.EmployerRepository;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.service.RegistrationService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationServiceImpl(UserRepository userRepository,
                                   EmployerRepository employerRepository,
                                   CandidateRepository candidateRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employerRepository = employerRepository;
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void register(RegistrationDto dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Email is already registered");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new ResponseStatusException(BAD_REQUEST, "Passwords do not match");
        }

        if (dto.getRole() != Role.EMPLOYER && dto.getRole() != Role.CANDIDATE) {
            throw new ResponseStatusException(BAD_REQUEST, "Only EMPLOYER or CANDIDATE registration is allowed");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(true);
        user.setStatus(com.narek.jobportal.entity.UserStatus.ACTIVE);
        user.setRoles(Set.of(dto.getRole()));

        User savedUser = userRepository.save(user);

        if (dto.getRole() == Role.EMPLOYER) {
            Employer employer = new Employer();
            employer.setCompanyName(dto.getCompanyName().trim());
            employer.setWebsite(normalizeOptional(dto.getWebsite()));
            employer.setUser(savedUser);
            employerRepository.save(employer);
            savedUser.setEmployer(employer);
        }

        if (dto.getRole() == Role.CANDIDATE) {
            Candidate candidate = new Candidate();
            candidate.setFullName(dto.getFullName().trim());
            candidate.setUser(savedUser);
            candidateRepository.save(candidate);
            savedUser.setCandidate(candidate);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
