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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void givenCandidateAlreadyApplied_whenCreateApplication_thenThrowConflict() {
        User user = TestEntityFactory.user(10L, "cand@mail.com", true, Role.CANDIDATE);
        Candidate candidate = TestEntityFactory.candidate(20L, user);
        Employer employer = TestEntityFactory.employer(30L, TestEntityFactory.user(11L, "emp@mail.com", true, Role.EMPLOYER));
        Job job = TestEntityFactory.job(1L, employer, 1000.0);
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(1L, "cover");

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(authService.getCurrentCandidate()).thenReturn(candidate);
        when(applicationRepository.existsByJobIdAndCandidateId(1L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(HttpStatus.CONFLICT.toString())
                .hasMessageContaining("already applied");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void givenValidApplication_whenCreateApplication_thenPersistAndReturnResponse() {
        User candidateUser = TestEntityFactory.user(10L, "cand@mail.com", true, Role.CANDIDATE);
        Candidate candidate = TestEntityFactory.candidate(20L, candidateUser);
        User employerUser = TestEntityFactory.user(11L, "emp@mail.com", true, Role.EMPLOYER);
        Employer employer = TestEntityFactory.employer(30L, employerUser);
        Job job = TestEntityFactory.job(1L, employer, 1000.0);

        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(1L, "   My cover letter   ");
        Application saved = TestEntityFactory.application(100L, job, candidate, ApplicationStatus.APPLIED);
        saved.setCoverLetter("My cover letter");

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(authService.getCurrentCandidate()).thenReturn(candidate);
        when(applicationRepository.existsByJobIdAndCandidateId(1L, 20L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);

        ApplicationResponseDto response = applicationService.createApplication(dto);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getJobId()).isEqualTo(1L);
        assertThat(response.getCandidateId()).isEqualTo(20L);
        assertThat(response.getCoverLetter()).isEqualTo("My cover letter");
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void givenAppliedStatus_whenAcceptApplication_thenSetAccepted() {
        Application application = TestEntityFactory.application(1L,
                TestEntityFactory.job(2L, TestEntityFactory.employer(3L, TestEntityFactory.user(4L, "emp@mail.com", true, Role.EMPLOYER)), 1000),
                TestEntityFactory.candidate(5L, TestEntityFactory.user(6L, "cand@mail.com", true, Role.CANDIDATE)),
                ApplicationStatus.APPLIED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponseDto result = applicationService.acceptApplication(1L);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(applicationRepository).save(application);
    }

    @Test
    void givenFinalizedStatus_whenRejectApplication_thenThrowBadRequest() {
        Application application = TestEntityFactory.application(1L,
                TestEntityFactory.job(2L, TestEntityFactory.employer(3L, TestEntityFactory.user(4L, "emp@mail.com", true, Role.EMPLOYER)), 1000),
                TestEntityFactory.candidate(5L, TestEntityFactory.user(6L, "cand@mail.com", true, Role.CANDIDATE)),
                ApplicationStatus.ACCEPTED);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.rejectApplication(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(HttpStatus.BAD_REQUEST.toString())
                .hasMessageContaining("already finalized");

        verify(applicationRepository, never()).save(any());
    }
}