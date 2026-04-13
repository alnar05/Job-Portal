package com.narek.jobportal.service;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Employer;
import java.util.Optional;

public interface EmployerService {
    Employer saveEmployer(Employer employer);
    Optional<Employer> getEmployerById(Long id);
    Optional<Employer> getEmployerByUserId(Long userId);
    Optional<Employer> getEmployerByUserEmail(String email);
    Employer updateOwnProfile(ProfileUpdateDto profileUpdateDto, String authenticatedEmail);
}
