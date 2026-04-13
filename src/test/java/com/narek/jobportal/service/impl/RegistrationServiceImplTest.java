package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.RegistrationDto;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.CandidateRepository;
import com.narek.jobportal.repository.EmployerRepository;
import com.narek.jobportal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployerRepository employerRepository;
    @Mock private CandidateRepository candidateRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void givenExistingEmail_whenRegister_thenThrowConflict() {
        RegistrationDto dto = candidateDto();
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(userRepository, never()).save(any());
    }

    @Test
    void givenPasswordMismatch_whenRegister_thenThrowBadRequest() {
        RegistrationDto dto = candidateDto();
        dto.setConfirmPassword("different");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    void givenDisallowedRole_whenRegister_thenThrowBadRequest() {
        RegistrationDto dto = candidateDto();
        dto.setRole(Role.ADMIN);
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only EMPLOYER or CANDIDATE");
    }

    @Test
    void givenEmployerRegistration_whenRegister_thenSaveEmployerProfile() {
        RegistrationDto dto = new RegistrationDto();
        dto.setEmail("emp@mail.com");
        dto.setPassword("Password123");
        dto.setConfirmPassword("Password123");
        dto.setRole(Role.EMPLOYER);
        dto.setCompanyName(" ACME ");
        dto.setWebsite("   ");

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        registrationService.register(dto);

        verify(employerRepository).save(argThat(emp -> emp.getCompanyName().equals("ACME") && emp.getWebsite() == null));
        verify(candidateRepository, never()).save(any());
    }

    @Test
    void givenCandidateRegistration_whenRegister_thenSaveCandidateProfile() {
        RegistrationDto dto = candidateDto();
        dto.setFullName(" Candidate ");
        dto.setResumeUrl("  ");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        registrationService.register(dto);

        verify(candidateRepository).save(argThat(c -> c.getFullName().equals("Candidate") && c.getResumeUrl() == null));
        verify(employerRepository, never()).save(any());
    }

    private RegistrationDto candidateDto() {
        RegistrationDto dto = new RegistrationDto();
        dto.setEmail("candidate@mail.com");
        dto.setPassword("Password123");
        dto.setConfirmPassword("Password123");
        dto.setRole(Role.CANDIDATE);
        dto.setFullName("Candidate");
        return dto;
    }
}