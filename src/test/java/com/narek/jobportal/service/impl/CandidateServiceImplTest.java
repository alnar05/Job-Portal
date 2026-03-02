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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateServiceImplTest {

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private CandidateServiceImpl candidateService;

    @Test
    void givenAuthenticatedCandidate_whenUpdateOwnProfile_thenOnlyCandidateFieldsUpdated() {
        User user = TestEntityFactory.user(1L, "candidate@mail.com", true, Role.CANDIDATE);
        Candidate candidate = TestEntityFactory.candidate(10L, user);

        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName("Updated Name");
        dto.setResumeUrl("https://resume.example/new");
        dto.setCompanyName("Should not be applied");

        when(candidateRepository.findByUserEmail("candidate@mail.com")).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(candidate)).thenReturn(candidate);

        Candidate updated = candidateService.updateOwnProfile(dto, "candidate@mail.com");

        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getResumeUrl()).isEqualTo("https://resume.example/new");
        assertThat(updated.getUser().getEmail()).isEqualTo("candidate@mail.com");
    }
}