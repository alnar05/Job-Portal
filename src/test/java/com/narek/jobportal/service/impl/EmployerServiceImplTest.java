package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.EmployerRepository;
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
class EmployerServiceImplTest {

    @Mock
    private EmployerRepository employerRepository;

    @InjectMocks
    private EmployerServiceImpl employerService;

    @Test
    void saveEmployer_shouldPersistAndReturnSavedEmployer_whenInputIsValid() {
        Employer input = buildEmployer(null, "Acme Inc", 101L);
        Employer saved = buildEmployer(1L, "Acme Inc", 101L);
        given(employerRepository.save(input)).willReturn(saved);

        Employer result = employerService.saveEmployer(input);

        assertEquals(1L, result.getId());
        assertEquals("Acme Inc", result.getCompanyName());
        assertEquals(101L, result.getUser().getId());
        verify(employerRepository).save(input);
    }

    @Test
    void saveEmployer_shouldPropagateException_whenRepositoryFails() {
        Employer input = buildEmployer(null, "Acme Inc", 101L);
        given(employerRepository.save(input)).willThrow(new IllegalStateException("constraint violation"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> employerService.saveEmployer(input));

        assertEquals("constraint violation", exception.getMessage());
        verify(employerRepository).save(input);
    }

    @Test
    void getEmployerById_shouldReturnEmployer_whenEmployerExists() {
        Employer employer = buildEmployer(5L, "Globex", 500L);
        given(employerRepository.findById(5L)).willReturn(Optional.of(employer));

        Optional<Employer> result = employerService.getEmployerById(5L);

        assertTrue(result.isPresent());
        assertEquals("Globex", result.get().getCompanyName());
        verify(employerRepository).findById(5L);
    }

    @Test
    void getEmployerById_shouldReturnEmpty_whenEmployerDoesNotExist() {
        given(employerRepository.findById(55L)).willReturn(Optional.empty());

        Optional<Employer> result = employerService.getEmployerById(55L);

        assertTrue(result.isEmpty());
        verify(employerRepository).findById(55L);
    }

    @Test
    void getEmployerByUserId_shouldReturnEmployer_whenMappingExists() {
        Employer employer = buildEmployer(7L, "Initech", 42L);
        given(employerRepository.findByUserId(42L)).willReturn(Optional.of(employer));

        Optional<Employer> result = employerService.getEmployerByUserId(42L);

        assertTrue(result.isPresent());
        assertEquals(7L, result.get().getId());
        verify(employerRepository).findByUserId(42L);
    }

    @Test
    void getEmployerByUserId_shouldReturnEmpty_whenMappingDoesNotExist() {
        given(employerRepository.findByUserId(404L)).willReturn(Optional.empty());

        Optional<Employer> result = employerService.getEmployerByUserId(404L);

        assertTrue(result.isEmpty());
        verify(employerRepository).findByUserId(404L);
    }

    private static Employer buildEmployer(Long employerId, String companyName, Long userId) {
        User user = new User();
        user.setId(userId);

        Employer employer = new Employer();
        employer.setId(employerId);
        employer.setCompanyName(companyName);
        employer.setUser(user);
        return employer;
    }
}