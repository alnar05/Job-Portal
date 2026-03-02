package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.testsupport.TestEntityFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void givenAdminDisablesUser_whenSetEnabledFalse_thenPersistDisabledFlag() {
        User user = TestEntityFactory.user(1L, "candidate@mail.com", true, Role.CANDIDATE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.setEnabled(1L, false);

        verify(userRepository).save(argThat(saved -> !saved.isEnabled()));
    }

    @Test
    void givenMissingUser_whenSetEnabled_thenThrow() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setEnabled(1L, false)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void givenCandidateUser_whenDeleteUser_thenDeleteCandidateApplicationsFirst() {
        User user = TestEntityFactory.user(1L, "candidate@mail.com", true, Role.CANDIDATE);
        Candidate candidate = TestEntityFactory.candidate(30L, user);
        user.setCandidate(candidate);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(applicationRepository).deleteByCandidateId(30L);
        verify(userRepository).delete(user);
    }

    @Test
    void givenNonCandidateUser_whenDeleteUser_thenSkipApplicationDeletion() {
        User user = TestEntityFactory.user(1L, "employer@mail.com", true, Role.EMPLOYER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(applicationRepository, never()).deleteByCandidateId(any());
        verify(userRepository).delete(user);
    }
}