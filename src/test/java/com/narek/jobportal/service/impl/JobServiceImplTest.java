package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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

    @Test
    void givenJobsExist_whenGetAllJobs_thenReturnMappedDtos() {
        when(jobRepository.findAll()).thenReturn(List.of(job(1L, 1000d), job(2L, 2000d)));

        List<JobResponseDto> results = jobService.getAllJobs();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCompanyName()).isNotBlank();
    }

    @Test
    void givenJobMissing_whenGetJobById_thenThrow() {
        when(jobRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> jobService.getJobById(9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    void givenEmployerId_whenGetJobsByEmployerId_thenReturnJobs() {
        when(jobRepository.findByEmployerId(2L)).thenReturn(List.of(job(1L, 1100d)));

        assertThat(jobService.getJobsByEmployerId(2L)).hasSize(1);
    }

    @Test
    void givenExistingJob_whenDeleteJob_thenDeleteApplicationsBeforeJob() {
        Job job = job(10L, 2000d);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        jobService.deleteJob(10L);

        verify(applicationRepository).deleteByJobId(10L);
        verify(jobRepository).delete(job);
    }

    @Test
    void givenMissingJob_whenDeleteJob_thenThrow() {
        when(jobRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.deleteJob(10L)).isInstanceOf(RuntimeException.class);
        verify(applicationRepository, never()).deleteByJobId(any());
    }

    @Test
    void givenValidDto_whenCreateJob_thenJobSavedForCurrentEmployer() {
        Employer employer = TestEntityFactory.employer(2L, TestEntityFactory.user(1L, "emp@mail.com", true, Role.EMPLOYER));
        Job persisted = job(10L, 2000d);
        persisted.setTitle("Java Dev");
        persisted.setDescription("desc");

        when(authService.getCurrentEmployer()).thenReturn(employer);
        when(jobRepository.save(any(Job.class))).thenReturn(persisted);

        JobResponseDto response = jobService.createJob(new JobCreateUpdateDto("Java Dev", "desc", 2000.0));

        assertThat(response.getId()).isEqualTo(10L);
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void givenExistingJob_whenUpdateJob_thenPersistUpdatedValues() {
        Job existing = job(5L, 900d);
        when(jobRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(jobRepository.save(existing)).thenReturn(existing);

        JobResponseDto result = jobService.updateJob(5L, new JobCreateUpdateDto("New", "New desc", 3000d));

        assertThat(result.getTitle()).isEqualTo("New");
        assertThat(result.getSalary()).isEqualTo(3000d);
    }

    @Test
    void givenMissingJob_whenUpdateJob_thenThrow() {
        when(jobRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateJob(5L, new JobCreateUpdateDto("a", "b", 1d)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Job not found");
    }

    private Job job(Long id, Double salary) {
        Employer employer = TestEntityFactory.employer(2L, TestEntityFactory.user(1L, "emp@mail.com", true, Role.EMPLOYER));
        return TestEntityFactory.job(id, employer, salary);
    }
}