package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.*;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    void givenAuthenticatedCandidate_whenGetCurrentCandidate_thenReturnCandidate() {
        User user = TestEntityFactory.user(1L, "candidate@mail.com", true, Role.CANDIDATE);
        Candidate candidate = TestEntityFactory.candidate(2L, user);
        user.setCandidate(candidate);
        authenticate("candidate@mail.com");
        when(userRepository.findByEmail("candidate@mail.com")).thenReturn(Optional.of(user));

        Candidate result = authService.getCurrentCandidate();

        assertThat(result.getId()).isEqualTo(2L);
    }

    @Test
    void givenCandidateUser_whenGetCurrentEmployer_thenThrow() {
        User user = TestEntityFactory.user(1L, "candidate@mail.com", true, Role.CANDIDATE);
        user.setCandidate(TestEntityFactory.candidate(2L, user));
        authenticate("candidate@mail.com");
        when(userRepository.findByEmail("candidate@mail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.getCurrentEmployer()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenCurrentCandidateAndSameId_whenIsCurrentCandidate_thenTrue() {
        User user = TestEntityFactory.user(1L, "candidate@mail.com", true, Role.CANDIDATE);
        user.setCandidate(TestEntityFactory.candidate(7L, user));
        authenticate("candidate@mail.com");
        when(userRepository.findByEmail("candidate@mail.com")).thenReturn(Optional.of(user));

        assertThat(authService.isCurrentCandidate(7L)).isTrue();
        assertThat(authService.isCurrentCandidate(8L)).isFalse();
    }

    @Test
    void givenOwnedJob_whenIsCurrentEmployerJob_thenTrue() {
        User user = TestEntityFactory.user(1L, "emp@mail.com", true, Role.EMPLOYER);
        Employer employer = TestEntityFactory.employer(9L, user);
        user.setEmployer(employer);
        Job job = TestEntityFactory.job(3L, employer, 1000d);
        authenticate("emp@mail.com");
        when(userRepository.findByEmail("emp@mail.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(3L)).thenReturn(Optional.of(job));

        assertThat(authService.isCurrentEmployerJob(3L)).isTrue();
    }

    @Test
    void givenMissingJob_whenIsCurrentEmployerJob_thenThrow() {
        when(jobRepository.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.isCurrentEmployerJob(3L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenOwnedApplicationByCandidate_whenIsCurrentCandidateApplication_thenTrue() {
        User user = TestEntityFactory.user(1L, "cand@mail.com", true, Role.CANDIDATE);
        Candidate candidate = TestEntityFactory.candidate(22L, user);
        user.setCandidate(candidate);
        Application app = TestEntityFactory.application(1L, TestEntityFactory.job(2L, TestEntityFactory.employer(3L, TestEntityFactory.user(4L, "emp@mail.com", true, Role.EMPLOYER)), 1000d), candidate, ApplicationStatus.APPLIED);
        authenticate("cand@mail.com");
        when(userRepository.findByEmail("cand@mail.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThat(authService.isCurrentCandidateApplication(1L)).isTrue();
    }

    @Test
    void givenApplicationNotOwnedByEmployer_whenIsCurrentEmployerApplication_thenFalse() {
        User user = TestEntityFactory.user(1L, "emp@mail.com", true, Role.EMPLOYER);
        user.setEmployer(TestEntityFactory.employer(99L, user));
        Employer owner = TestEntityFactory.employer(77L, TestEntityFactory.user(2L, "owner@mail.com", true, Role.EMPLOYER));
        Application app = TestEntityFactory.application(1L, TestEntityFactory.job(2L, owner, 1000d), TestEntityFactory.candidate(3L, TestEntityFactory.user(4L, "cand@mail.com", true, Role.CANDIDATE)), ApplicationStatus.APPLIED);
        authenticate("emp@mail.com");
        when(userRepository.findByEmail("emp@mail.com")).thenReturn(Optional.of(user));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));

        assertThat(authService.isCurrentEmployerApplication(1L)).isFalse();
    }

    private void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, "n/a"));
    }
}