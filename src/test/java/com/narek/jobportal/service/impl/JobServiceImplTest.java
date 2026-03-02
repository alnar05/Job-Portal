package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.*;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.testsupport.TestEntityFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private AuthService authService;

    @InjectMocks
    private JobServiceImpl jobService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void givenNegativeSalary_whenValidateDto_thenValidationFails() {
        JobCreateUpdateDto dto = new JobCreateUpdateDto("Java Dev", "desc", -500.0);

        assertThat(validator.validate(dto))
                .anyMatch(v -> v.getPropertyPath().toString().equals("salary"));
    }

    @Test
    void givenValidDto_whenCreateJob_thenJobSavedForCurrentEmployer() {
        User user = TestEntityFactory.user(1L, "emp@mail.com", true, Role.EMPLOYER);
        Employer employer = TestEntityFactory.employer(2L, user);
        JobCreateUpdateDto dto = new JobCreateUpdateDto("Java Dev", "desc", 2000.0);

        Job persisted = TestEntityFactory.job(10L, employer, 2000.0);
        persisted.setTitle("Java Dev");
        persisted.setDescription("desc");

        when(authService.getCurrentEmployer()).thenReturn(employer);
        when(jobRepository.save(any(Job.class))).thenReturn(persisted);

        JobResponseDto response = jobService.createJob(dto);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Java Dev");
        assertThat(response.getCompanyName()).isEqualTo(employer.getCompanyName());
    }

    @Test
    void givenExistingJob_whenDeleteJob_thenDeleteApplicationsBeforeJob() {
        Job job = TestEntityFactory.job(10L, TestEntityFactory.employer(2L,
                TestEntityFactory.user(1L, "emp@mail.com", true, Role.EMPLOYER)), 2000.0);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        jobService.deleteJob(10L);

        verify(applicationRepository).deleteByJobId(10L);
        verify(jobRepository).delete(job);
    }
}