package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CandidateServiceImplTest {

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private CandidateServiceImpl candidateService;

    @Test
    void saveCandidate_shouldPersistAndReturnSavedCandidate_whenInputIsValid() {
        Candidate input = buildCandidate(null, "John Candidate", 11L);
        Candidate saved = buildCandidate(1L, "John Candidate", 11L);
        given(candidateRepository.save(input)).willReturn(saved);

        Candidate result = candidateService.saveCandidate(input);

        assertEquals(1L, result.getId());
        assertEquals("John Candidate", result.getFullName());
        assertEquals(11L, result.getUser().getId());
        verify(candidateRepository).save(input);
    }

    @Test
    void saveCandidate_shouldPropagateException_whenRepositoryFails() {
        Candidate input = buildCandidate(null, "Jane Candidate", 10L);
        given(candidateRepository.save(input)).willThrow(new IllegalArgumentException("invalid candidate"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> candidateService.saveCandidate(input));

        assertEquals("invalid candidate", exception.getMessage());
        verify(candidateRepository).save(input);
    }

    @Test
    void getCandidateById_shouldReturnCandidate_whenCandidateExists() {
        Candidate candidate = buildCandidate(9L, "Alice", 100L);
        given(candidateRepository.findById(9L)).willReturn(Optional.of(candidate));

        Optional<Candidate> result = candidateService.getCandidateById(9L);

        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getFullName());
        verify(candidateRepository).findById(9L);
    }

    @Test
    void getCandidateById_shouldReturnEmpty_whenCandidateDoesNotExist() {
        given(candidateRepository.findById(99L)).willReturn(Optional.empty());

        Optional<Candidate> result = candidateService.getCandidateById(99L);

        assertTrue(result.isEmpty());
        verify(candidateRepository).findById(99L);
    }

    @Test
    void getCandidateByUserId_shouldReturnCandidate_whenMappingExists() {
        Candidate candidate = buildCandidate(7L, "Bob", 88L);
        given(candidateRepository.findByUserId(88L)).willReturn(Optional.of(candidate));

        Optional<Candidate> result = candidateService.getCandidateByUserId(88L);

        assertTrue(result.isPresent());
        assertEquals(7L, result.get().getId());
        verify(candidateRepository).findByUserId(88L);
    }

    @Test
    void getCandidateByUserId_shouldReturnEmpty_whenMappingDoesNotExist() {
        given(candidateRepository.findByUserId(404L)).willReturn(Optional.empty());

        Optional<Candidate> result = candidateService.getCandidateByUserId(404L);

        assertTrue(result.isEmpty());
        verify(candidateRepository).findByUserId(404L);
    }

    private static Candidate buildCandidate(Long candidateId, String fullName, Long userId) {
        User user = new User();
        user.setId(userId);

        Candidate candidate = new Candidate();
        candidate.setId(candidateId);
        candidate.setFullName(fullName);
        candidate.setUser(user);
        return candidate;
    }
}