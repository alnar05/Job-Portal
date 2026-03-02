package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.entity.*;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private JobRepository jobRepository;
    @Mock private AuthService authService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void givenMissingJob_whenCreateApplication_thenThrowNotFound() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(new ApplicationCreateUpdateDto(99L, "cover")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    void givenCandidateAlreadyApplied_whenCreateApplication_thenThrowConflict() {
        Candidate candidate = candidate();
        Job job = job();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(authService.getCurrentCandidate()).thenReturn(candidate);
        when(applicationRepository.existsByJobIdAndCandidateId(1L, candidate.getId())).thenReturn(true);
        assertThatThrownBy(() -> applicationService.createApplication(new ApplicationCreateUpdateDto(1L, "cover")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(HttpStatus.CONFLICT.toString())
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void givenBlankCoverLetter_whenCreateApplication_thenPersistNullCoverLetter() {
        Candidate candidate = candidate();
        Job job = job();
        Application saved = TestEntityFactory.application(12L, job, candidate, ApplicationStatus.APPLIED);
        saved.setCoverLetter(null);

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(authService.getCurrentCandidate()).thenReturn(candidate);
        when(applicationRepository.existsByJobIdAndCandidateId(1L, candidate.getId())).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);

        ApplicationResponseDto response = applicationService.createApplication(new ApplicationCreateUpdateDto(1L, "   "));

        assertThat(response.getCoverLetter()).isNull();
    }

    @Test
    void givenValidApplication_whenCreateApplication_thenPersistAndReturnResponse() {
        Candidate candidate = candidate();
        Job job = job();
        Application saved = TestEntityFactory.application(100L, job, candidate, ApplicationStatus.APPLIED);
        saved.setCoverLetter("My cover letter");

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(authService.getCurrentCandidate()).thenReturn(candidate);
        when(applicationRepository.existsByJobIdAndCandidateId(1L, candidate.getId())).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);

        ApplicationResponseDto response = applicationService.createApplication(new ApplicationCreateUpdateDto(1L, "  My cover letter  "));

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void givenUpdateCall_whenUpdateApplication_thenThrowUnsupportedOperationException() {
        assertThatThrownBy(() -> applicationService.updateApplication(1L, new ApplicationCreateUpdateDto()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void givenApplicationNotFound_whenDeleteApplication_thenThrowNotFound() {
        when(applicationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.deleteApplication(5L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void givenApplicationExists_whenDeleteApplication_thenDeleteIt() {
        Application application = application(10L, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        applicationService.deleteApplication(10L);

        verify(applicationRepository).delete(application);
    }

    @Test
    void givenApplicationNotFound_whenGetApplicationById_thenThrowNotFound() {
        when(applicationRepository.findById(6L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(6L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void givenApplications_whenGetAllApplications_thenMapAll() {
        when(applicationRepository.findAll()).thenReturn(List.of(application(1L, ApplicationStatus.APPLIED), application(2L, ApplicationStatus.REVIEWED)));

        List<ApplicationResponseDto> results = applicationService.getAllApplications();

        assertThat(results).hasSize(2);
    }

    @Test
    void givenAppliedStatus_whenMarkAsReviewed_thenSetReviewed() {
        Application application = application(7L, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponseDto result = applicationService.markAsReviewed(7L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REVIEWED);
    }

    @Test
    void givenNonAppliedStatus_whenMarkAsReviewed_thenKeepCurrentStatus() {
        Application application = application(7L, ApplicationStatus.REVIEWED);
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponseDto result = applicationService.markAsReviewed(7L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REVIEWED);
    }

    @Test
    void givenAppliedStatus_whenAcceptApplication_thenSetAccepted() {
        Application application = application(1L, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponseDto result = applicationService.acceptApplication(1L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);

    }

    @Test
    void givenReviewedStatus_whenRejectApplication_thenSetRejected() {
        Application application = application(1L, ApplicationStatus.REVIEWED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponseDto result = applicationService.rejectApplication(1L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }
    @Test
    void givenFinalizedStatus_whenAcceptApplication_thenThrowBadRequest() {
        Application application = application(1L, ApplicationStatus.ACCEPTED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.acceptApplication(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("already finalized");
    }

    private Candidate candidate() {
        return TestEntityFactory.candidate(20L, TestEntityFactory.user(10L, "cand@mail.com", true, Role.CANDIDATE));
    }

    private Job job() {
        return TestEntityFactory.job(1L, TestEntityFactory.employer(30L, TestEntityFactory.user(11L, "emp@mail.com", true, Role.EMPLOYER)), 1000.0);
    }

    private Application application(Long id, ApplicationStatus status) {
        return TestEntityFactory.application(id, job(), candidate(), status);
    }
}