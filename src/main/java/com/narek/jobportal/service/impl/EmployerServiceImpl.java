package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.repository.EmployerRepository;
import com.narek.jobportal.service.EmployerService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EmployerServiceImpl implements EmployerService {

    private final EmployerRepository employerRepository;

    public EmployerServiceImpl(EmployerRepository employerRepository) {
        this.employerRepository = employerRepository;
    }

    @Override
    public Employer saveEmployer(Employer employer) {
        return employerRepository.save(employer);
    }

    @Override
    public Optional<Employer> getEmployerById(Long id) {
        return employerRepository.findById(id);
    }

    @Override
    public Optional<Employer> getEmployerByUserId(Long userId) {
        return employerRepository.findByUserId(userId);
    }

    @Override
    public Optional<Employer> getEmployerByUserEmail(String email) {
        return employerRepository.findByUserEmail(email);
    }

    @Override
    @Transactional
    public Employer updateOwnProfile(ProfileUpdateDto profileUpdateDto, String authenticatedEmail) {
        Employer employer = employerRepository.findByUserEmail(authenticatedEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employer profile not found for email: " + authenticatedEmail));

        if (!employer.getUser().getEmail().equals(authenticatedEmail)) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        employer.setCompanyName(profileUpdateDto.getCompanyName());
        employer.setWebsite(profileUpdateDto.getWebsite());

        return employerRepository.save(employer);
    }
}
