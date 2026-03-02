package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.testsupport.TestEntityFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private JobRepository jobRepository;
    @Mock private ApplicationRepository applicationRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNoAuthentication_whenGetCurrentCandidate_thenThrow() {
        assertThatThrownBy(() -> authService.getCurrentCandidate())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Authenticated user not found");
    }

    @Test
    void givenAuthenticatedEmployer_whenGetCurrentCandidate_thenThrowNotCandidate() {
        User employerUser = TestEntityFactory.user(1L, "employer@mail.com", true, Role.EMPLOYER);
        employerUser.setEmployer(TestEntityFactory.employer(2L, employerUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("employer@mail.com", "n/a", java.util.List.of())
        );
        when(userRepository.findByEmail("employer@mail.com")).thenReturn(Optional.of(employerUser));

        assertThatThrownBy(() -> authService.getCurrentCandidate())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not a candidate");
    }
}