package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.EmployerRepository;
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
class EmployerServiceImplTest {

    @Mock
    private EmployerRepository employerRepository;

    @InjectMocks
    private EmployerServiceImpl employerService;

    @Test
    void givenAuthenticatedEmployer_whenUpdateOwnProfile_thenOnlyEmployerFieldsUpdated() {
        User user = TestEntityFactory.user(1L, "employer@mail.com", true, Role.EMPLOYER);
        Employer employer = TestEntityFactory.employer(10L, user);

        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setCompanyName("Updated Company");
        dto.setWebsite("https://updated.example");
        dto.setFullName("Should not be applied");

        when(employerRepository.findByUserEmail("employer@mail.com")).thenReturn(Optional.of(employer));
        when(employerRepository.save(employer)).thenReturn(employer);

        Employer updated = employerService.updateOwnProfile(dto, "employer@mail.com");

        assertThat(updated.getCompanyName()).isEqualTo("Updated Company");
        assertThat(updated.getWebsite()).isEqualTo("https://updated.example");
        assertThat(updated.getUser().getEmail()).isEqualTo("employer@mail.com");
    }
}