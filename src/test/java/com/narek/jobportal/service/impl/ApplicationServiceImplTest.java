package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.exception.DuplicateApplicationException;
import com.narek.jobportal.exception.JobApplicationClosedException;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void createApplication_shouldCreateApplicationSuccessfully_whenInputIsValid() {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(10L, "I am a strong fit for this role.");
        Job job = buildJob(10L, "Senior Java Developer", LocalDate.now().plusDays(5));
        Candidate candidate = buildCandidate(7L, "candidate@example.com");

        Application savedApplication = new Application();
        savedApplication.setId(100L);
        savedApplication.setJob(job);
        savedApplication.setCandidate(candidate);
        savedApplication.setCoverLetter(dto.getCoverLetter());
        savedApplication.setAppliedAt(LocalDateTime.of(2026, 1, 1, 12, 0));

        given(jobRepository.findById(dto.getJobId())).willReturn(Optional.of(job));
        given(authService.getCurrentCandidate()).willReturn(candidate);
        given(applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())).willReturn(false);
        given(applicationRepository.save(any(Application.class))).willReturn(savedApplication);

        ApplicationResponseDto result = applicationService.createApplication(dto);

        assertNotNull(result);
        assertEquals(savedApplication.getId(), result.getId());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void createApplication_shouldThrow_whenJobExpired() {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(10L, "Second attempt");
        Job job = buildJob(10L, "Backend Engineer", LocalDate.now().minusDays(1));
        Candidate candidate = buildCandidate(5L, "candidate@example.com");

        given(jobRepository.findById(dto.getJobId())).willReturn(Optional.of(job));
        given(authService.getCurrentCandidate()).willReturn(candidate);

        JobApplicationClosedException exception = assertThrows(
                JobApplicationClosedException.class,
                () -> applicationService.createApplication(dto)
        );

        assertEquals("Job application period has closed", exception.getMessage());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_shouldThrowConflict_whenCandidateAlreadyAppliedToJob() {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(10L, "Second attempt");
        Job job = buildJob(10L, "Backend Engineer", LocalDate.now().plusDays(2));
        Candidate candidate = buildCandidate(5L, "candidate@example.com");

        given(jobRepository.findById(dto.getJobId())).willReturn(Optional.of(job));
        given(authService.getCurrentCandidate()).willReturn(candidate);
        given(applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())).willReturn(true);

        DuplicateApplicationException exception = assertThrows(
                DuplicateApplicationException.class,
                () -> applicationService.createApplication(dto)
        );

        assertEquals("You have already applied for job with id 10", exception.getMessage());
    }

    @Test
    void createApplication_shouldThrowRuntimeException_whenJobDoesNotExist() {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(999L, "Interested in this position");
        given(jobRepository.findById(dto.getJobId())).willReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> applicationService.createApplication(dto)
        );

        assertEquals("Job not found with id 999", exception.getMessage());
    }

    @Test
    void deleteApplication_shouldThrowNotFound_whenApplicationDoesNotExist() {
        given(applicationRepository.findById(77L)).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.deleteApplication(77L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private static Job buildJob(Long id, String title, LocalDate closingDate) {
        Job job = new Job();
        job.setId(id);
        job.setTitle(title);
        job.setClosingDate(closingDate);
        return job;
    }

    private static Candidate buildCandidate(Long id, String email) {
        User user = new User();
        user.setEmail(email);

        Candidate candidate = new Candidate();
        candidate.setId(id);
        candidate.setUser(user);
        return candidate;
    }
}
