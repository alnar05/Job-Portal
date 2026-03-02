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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        RegistrationDto dto = new RegistrationDto();
        dto.setEmail("exists@mail.com");
        dto.setPassword("pass");
        dto.setConfirmPassword("pass");
        dto.setRole(Role.CANDIDATE);

        when(userRepository.findByEmail("exists@mail.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(userRepository, never()).save(any());
    }
}