package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void getCurrentEmployer_shouldReturnEmployer_whenAuthenticatedUserHasEmployer() {
        setAuthenticatedEmail("employer@example.com");

        Employer employer = new Employer();
        employer.setId(1L);

        User user = new User();
        user.setEmail("employer@example.com");
        user.setEmployer(employer);

        given(userRepository.findByEmail("employer@example.com")).willReturn(Optional.of(user));

        Employer result = authService.getCurrentEmployer();

        assertEquals(1L, result.getId());
        verify(userRepository).findByEmail("employer@example.com");
    }

    @Test
    void getCurrentEmployer_shouldThrowRuntimeException_whenAuthenticatedUserNotFound() {
        setAuthenticatedEmail("missing@example.com");
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.getCurrentEmployer());

        assertEquals("Authenticated user not found", exception.getMessage());
        verify(userRepository).findByEmail("missing@example.com");
    }

    @Test
    void getCurrentEmployer_shouldThrowRuntimeException_whenUserIsNotEmployer() {
        setAuthenticatedEmail("candidate@example.com");

        User user = new User();
        user.setEmail("candidate@example.com");
        user.setEmployer(null);

        given(userRepository.findByEmail("candidate@example.com")).willReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.getCurrentEmployer());

        assertEquals("User is not an employer", exception.getMessage());
        verify(userRepository).findByEmail("candidate@example.com");
    }

    @Test
    void getCurrentCandidate_shouldReturnCandidate_whenAuthenticatedUserHasCandidate() {
        setAuthenticatedEmail("candidate@example.com");

        Candidate candidate = new Candidate();
        candidate.setId(20L);

        User user = new User();
        user.setEmail("candidate@example.com");
        user.setCandidate(candidate);

        given(userRepository.findByEmail("candidate@example.com")).willReturn(Optional.of(user));

        Candidate result = authService.getCurrentCandidate();

        assertEquals(20L, result.getId());
        verify(userRepository).findByEmail("candidate@example.com");
    }

    @Test
    void getCurrentCandidate_shouldThrowRuntimeException_whenAuthenticatedUserNotFound() {
        setAuthenticatedEmail("missing@example.com");
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.getCurrentCandidate());

        assertEquals("Authenticated user not found", exception.getMessage());
        verify(userRepository).findByEmail("missing@example.com");
    }

    @Test
    void getCurrentCandidate_shouldThrowRuntimeException_whenUserIsNotCandidate() {
        setAuthenticatedEmail("employer@example.com");

        User user = new User();
        user.setEmail("employer@example.com");
        user.setCandidate(null);

        given(userRepository.findByEmail("employer@example.com")).willReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.getCurrentCandidate());

        assertEquals("User is not a candidate", exception.getMessage());
        verify(userRepository).findByEmail("employer@example.com");
    }

    @Test
    void isCurrentCandidateApplication_shouldReturnTrue_whenOwnedByCurrentCandidate() {
        setAuthenticatedEmail("candidate@example.com");

        Candidate currentCandidate = new Candidate();
        currentCandidate.setId(3L);

        User user = new User();
        user.setEmail("candidate@example.com");
        user.setCandidate(currentCandidate);

        Candidate appOwner = new Candidate();
        appOwner.setId(3L);

        Application application = new Application();
        application.setId(100L);
        application.setCandidate(appOwner);

        given(applicationRepository.findById(100L)).willReturn(Optional.of(application));
        given(userRepository.findByEmail("candidate@example.com")).willReturn(Optional.of(user));

        boolean result = authService.isCurrentCandidateApplication(100L);

        assertTrue(result);
        verify(applicationRepository).findById(100L);
        verify(userRepository).findByEmail("candidate@example.com");
    }

    @Test
    void isCurrentCandidateApplication_shouldReturnFalse_whenApplicationBelongsToAnotherCandidate() {
        setAuthenticatedEmail("candidate@example.com");

        Candidate currentCandidate = new Candidate();
        currentCandidate.setId(3L);

        User user = new User();
        user.setEmail("candidate@example.com");
        user.setCandidate(currentCandidate);

        Candidate appOwner = new Candidate();
        appOwner.setId(9L);

        Application application = new Application();
        application.setId(100L);
        application.setCandidate(appOwner);

        given(applicationRepository.findById(100L)).willReturn(Optional.of(application));
        given(userRepository.findByEmail("candidate@example.com")).willReturn(Optional.of(user));

        boolean result = authService.isCurrentCandidateApplication(100L);

        assertFalse(result);
        verify(applicationRepository).findById(100L);
        verify(userRepository).findByEmail("candidate@example.com");
    }

    @Test
    void isCurrentCandidateApplication_shouldReturnFalse_whenApplicationNotFound() {
        // The implementation uses .orElse(false), so a missing application returns false, not an exception.
        given(applicationRepository.findById(100L)).willReturn(Optional.empty());

        boolean result = authService.isCurrentCandidateApplication(100L);

        assertFalse(result);
        verify(applicationRepository).findById(100L);
    }

    @Test
    void isCurrentEmployerJob_shouldReturnTrue_whenOwnedByCurrentEmployer() {
        setAuthenticatedEmail("employer@example.com");

        Employer currentEmployer = new Employer();
        currentEmployer.setId(8L);

        User user = new User();
        user.setEmail("employer@example.com");
        user.setEmployer(currentEmployer);

        Employer jobOwner = new Employer();
        jobOwner.setId(8L);

        Job job = new Job();
        job.setId(50L);
        job.setEmployer(jobOwner);

        given(jobRepository.findById(50L)).willReturn(Optional.of(job));
        given(userRepository.findByEmail("employer@example.com")).willReturn(Optional.of(user));

        boolean result = authService.isCurrentEmployerJob(50L);

        assertTrue(result);
        verify(jobRepository).findById(50L);
        verify(userRepository).findByEmail("employer@example.com");
    }

    @Test
    void isCurrentEmployerJob_shouldReturnFalse_whenJobBelongsToAnotherEmployer() {
        setAuthenticatedEmail("employer@example.com");

        Employer currentEmployer = new Employer();
        currentEmployer.setId(8L);

        User user = new User();
        user.setEmail("employer@example.com");
        user.setEmployer(currentEmployer);

        Employer jobOwner = new Employer();
        jobOwner.setId(99L);

        Job job = new Job();
        job.setId(50L);
        job.setEmployer(jobOwner);

        given(jobRepository.findById(50L)).willReturn(Optional.of(job));
        given(userRepository.findByEmail("employer@example.com")).willReturn(Optional.of(user));

        boolean result = authService.isCurrentEmployerJob(50L);

        assertFalse(result);
        verify(jobRepository).findById(50L);
        verify(userRepository).findByEmail("employer@example.com");
    }

    @Test
    void isCurrentEmployerJob_shouldThrowRuntimeException_whenJobNotFound() {
        // No auth setup needed — the method must throw before reaching the user lookup.
        given(jobRepository.findById(50L)).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.isCurrentEmployerJob(50L));

        assertEquals("Job not found", exception.getMessage());
        verify(jobRepository).findById(50L);
    }

    private void setAuthenticatedEmail(String email) {
        Authentication authentication = mock(Authentication.class);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn(email);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}