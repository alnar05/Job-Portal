package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
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
        Job job = buildJob(10L, "Senior Java Developer");
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
        assertEquals(job.getId(), result.getJobId());
        assertEquals(job.getTitle(), result.getJobTitle());
        assertEquals(candidate.getId(), result.getCandidateId());
        assertEquals(candidate.getUser().getEmail(), result.getCandidateName());
        assertEquals(dto.getCoverLetter(), result.getCoverLetter());
        assertEquals(savedApplication.getAppliedAt(), result.getAppliedAt());

        verify(jobRepository).findById(dto.getJobId());
        verify(authService).getCurrentCandidate();
        verify(applicationRepository).existsByJobIdAndCandidateId(job.getId(), candidate.getId());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void createApplication_shouldThrowConflict_whenCandidateAlreadyAppliedToJob() {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(10L, "Second attempt");
        Job job = buildJob(10L, "Backend Engineer");
        Candidate candidate = buildCandidate(5L, "candidate@example.com");

        given(jobRepository.findById(dto.getJobId())).willReturn(Optional.of(job));
        given(authService.getCurrentCandidate()).willReturn(candidate);
        given(applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())).willReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.createApplication(dto)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("You already applied to this job", exception.getReason());

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_shouldThrowRuntimeException_whenJobDoesNotExist() {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(999L, "Interested in this position");
        given(jobRepository.findById(dto.getJobId())).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> applicationService.createApplication(dto)
        );

        assertEquals("Job not found", exception.getMessage());
        verify(authService, never()).getCurrentCandidate();
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_shouldPropagateException_whenAuthenticationFails() {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(10L, "Cover letter");
        Job job = buildJob(10L, "Java Developer");

        given(jobRepository.findById(dto.getJobId())).willReturn(Optional.of(job));
        given(authService.getCurrentCandidate()).willThrow(new IllegalStateException("No authenticated candidate"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> applicationService.createApplication(dto)
        );

        assertEquals("No authenticated candidate", exception.getMessage());
        verify(applicationRepository, never()).existsByJobIdAndCandidateId(any(), any());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void createApplication_shouldThrowNullPointerException_whenDtoIsNull() {
        assertThrows(NullPointerException.class, () -> applicationService.createApplication(null));
        verify(jobRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void updateApplication_shouldThrowUnsupportedOperationException() {
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(1L, "New cover letter");

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> applicationService.updateApplication(1L, dto)
        );

        assertEquals("Updating applications is not allowed", exception.getMessage());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void deleteApplication_shouldDeleteSuccessfully_whenApplicationExists() {
        Application application = new Application();
        application.setId(77L);

        given(applicationRepository.findById(77L)).willReturn(Optional.of(application));

        applicationService.deleteApplication(77L);

        verify(applicationRepository).findById(77L);
        verify(applicationRepository).delete(application);
    }

    @Test
    void deleteApplication_shouldThrowNotFound_whenApplicationDoesNotExist() {
        given(applicationRepository.findById(77L)).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.deleteApplication(77L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Application not found", exception.getReason());
        verify(applicationRepository, never()).delete(any(Application.class));
    }

    @Test
    void getApplicationById_shouldReturnMappedDto_whenApplicationExists() {
        Application application = buildApplication(50L, 9L, "QA Engineer", 3L, "qa.candidate@example.com", "My cover letter");

        given(applicationRepository.findById(50L)).willReturn(Optional.of(application));

        ApplicationResponseDto result = applicationService.getApplicationById(50L);

        assertEquals(application.getId(), result.getId());
        assertEquals(application.getJob().getId(), result.getJobId());
        assertEquals(application.getJob().getTitle(), result.getJobTitle());
        assertEquals(application.getCandidate().getId(), result.getCandidateId());
        assertEquals(application.getCandidate().getUser().getEmail(), result.getCandidateName());
        assertEquals(application.getCoverLetter(), result.getCoverLetter());
        assertEquals(application.getAppliedAt(), result.getAppliedAt());

        verify(applicationRepository).findById(50L);
    }

    @Test
    void getApplicationById_shouldThrowNotFound_whenApplicationMissing() {
        given(applicationRepository.findById(50L)).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationService.getApplicationById(50L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Application not found", exception.getReason());
    }

    @Test
    void getAllApplications_shouldReturnMappedDtos() {
        Application first = buildApplication(1L, 100L, "Java Engineer", 11L, "a@example.com", "First");
        Application second = buildApplication(2L, 101L, "DevOps Engineer", 12L, "b@example.com", "Second");

        given(applicationRepository.findAll()).willReturn(List.of(first, second));

        List<ApplicationResponseDto> results = applicationService.getAllApplications();

        assertEquals(2, results.size());
        assertEquals("Java Engineer", results.get(0).getJobTitle());
        assertEquals("b@example.com", results.get(1).getCandidateName());

        verify(applicationRepository).findAll();
    }

    @Test
    void getApplicationsByCandidateId_shouldReturnOnlyCandidateApplications() {
        Long candidateId = 44L;
        Application first = buildApplication(1L, 100L, "Java Engineer", candidateId, "a@example.com", "First");
        Application second = buildApplication(2L, 101L, "DevOps Engineer", candidateId, "a@example.com", "Second");

        given(applicationRepository.findByCandidateId(candidateId)).willReturn(List.of(first, second));

        List<ApplicationResponseDto> results = applicationService.getApplicationsByCandidateId(candidateId);

        assertEquals(2, results.size());
        assertEquals(candidateId, results.get(0).getCandidateId());
        assertEquals(candidateId, results.get(1).getCandidateId());

        verify(applicationRepository).findByCandidateId(candidateId);
    }

    @Test
    void getApplicationsByJobId_shouldReturnOnlyJobApplications() {
        Long jobId = 77L;
        Application first = buildApplication(1L, jobId, "Java Engineer", 10L, "first@example.com", "First");
        Application second = buildApplication(2L, jobId, "Java Engineer", 11L, "second@example.com", "Second");

        given(applicationRepository.findByJobId(jobId)).willReturn(List.of(first, second));

        List<ApplicationResponseDto> results = applicationService.getApplicationsByJobId(jobId);

        assertEquals(2, results.size());
        assertEquals(jobId, results.get(0).getJobId());
        assertEquals(jobId, results.get(1).getJobId());

        verify(applicationRepository).findByJobId(jobId);
    }

    private static Job buildJob(Long id, String title) {
        Job job = new Job();
        job.setId(id);
        job.setTitle(title);
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

    private static Application buildApplication(Long applicationId,
                                                Long jobId,
                                                String jobTitle,
                                                Long candidateId,
                                                String candidateEmail,
                                                String coverLetter) {
        Application application = new Application();
        application.setId(applicationId);
        application.setJob(buildJob(jobId, jobTitle));
        application.setCandidate(buildCandidate(candidateId, candidateEmail));
        application.setCoverLetter(coverLetter);
        application.setAppliedAt(LocalDateTime.of(2026, 2, 1, 9, 30));
        return application;
    }
}