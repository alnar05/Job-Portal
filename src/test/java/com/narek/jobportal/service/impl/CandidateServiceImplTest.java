package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.repository.CandidateRepository;
import com.narek.jobportal.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceImplTest {

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private CandidateServiceImpl candidateService;

    @Test
    void givenCandidateNotFound_whenUpdateOwnProfile_thenThrow() {
        when(candidateRepository.findByUserEmail("candidate@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateService.updateOwnProfile(new ProfileUpdateDto(), "candidate@mail.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void givenAuthenticatedEmailMismatch_whenUpdateOwnProfile_thenDenyAccess() {
        User user = TestEntityFactory.user(1L, "other@mail.com", true, Role.CANDIDATE);
        Candidate candidate = TestEntityFactory.candidate(10L, user);
        when(candidateRepository.findByUserEmail("candidate@mail.com")).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> candidateService.updateOwnProfile(new ProfileUpdateDto(), "candidate@mail.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void givenAuthenticatedCandidate_whenUpdateOwnProfile_thenOnlyCandidateFieldsUpdated() {
        User user = TestEntityFactory.user(1L, "candidate@mail.com", true, Role.CANDIDATE);
        Candidate candidate = TestEntityFactory.candidate(10L, user);

        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName("Updated Name");
        dto.setResumeUrl("https://resume.example/new");
        dto.setCompanyName("Must not be copied");

        when(candidateRepository.findByUserEmail("candidate@mail.com")).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(candidate)).thenReturn(candidate);

        Candidate updated = candidateService.updateOwnProfile(dto, "candidate@mail.com");

        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getResumeUrl()).isEqualTo("https://resume.example/new");
        verify(candidateRepository).save(candidate);;
    }
}