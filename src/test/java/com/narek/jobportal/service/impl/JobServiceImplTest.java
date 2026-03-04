package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private AuthService authService;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void getAllJobs_shouldReturnMappedDtos() {
        Job first = buildJob(1L, "Java Engineer", "Desc1", 100_000d, buildEmployer(10L, "Acme"));
        Job second = buildJob(2L, "DevOps", "Desc2", 90_000d, buildEmployer(11L, "Globex"));
        given(jobRepository.findAll()).willReturn(List.of(first, second));

        List<JobResponseDto> result = jobService.getAllJobs();

        assertEquals(2, result.size());
        assertEquals("Java Engineer", result.get(0).getTitle());
        assertEquals("Globex", result.get(1).getCompanyName());
        verify(jobRepository).findAll();
    }

    @Test
    void getJobById_shouldReturnMappedDto_whenJobExists() {
        Job job = buildJob(5L, "Backend Engineer", "API role", 120_000d, buildEmployer(20L, "Tech Corp"));
        given(jobRepository.findById(5L)).willReturn(Optional.of(job));

        JobResponseDto result = jobService.getJobById(5L);

        assertEquals(5L, result.getId());
        assertEquals("Backend Engineer", result.getTitle());
        assertEquals("Tech Corp", result.getCompanyName());
        verify(jobRepository).findById(5L);
    }

    @Test
    void getJobById_shouldThrowException_whenJobMissing() {
        given(jobRepository.findById(99L)).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> jobService.getJobById(99L));

        assertEquals("Job not found", exception.getMessage());
        verify(jobRepository).findById(99L);
    }

    @Test
    void getJobsByEmployerId_shouldReturnMappedDtos() {
        Long employerId = 30L;
        Employer employer = buildEmployer(employerId, "Employer A");
        given(jobRepository.findByEmployerId(employerId)).willReturn(List.of(
                buildJob(1L, "A", "A", 1d, employer),
                buildJob(2L, "B", "B", 2d, employer)
        ));

        List<JobResponseDto> result = jobService.getJobsByEmployerId(employerId);

        assertEquals(2, result.size());
        assertEquals("Employer A", result.get(0).getCompanyName());
        verify(jobRepository).findByEmployerId(employerId);
    }

    @Test
    void deleteJob_shouldDelete_whenJobExists() {
        Job job = buildJob(7L, "SRE", "Ops", 110_000d, buildEmployer(1L, "Ops Inc"));
        given(jobRepository.findById(7L)).willReturn(Optional.of(job));

        jobService.deleteJob(7L);

        verify(applicationRepository).deleteByJobId(7L);
        verify(jobRepository).delete(job);
    }

    @Test
    void deleteJob_shouldThrow_whenJobMissing() {
        given(jobRepository.findById(7L)).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> jobService.deleteJob(7L));

        assertEquals("Job not found", exception.getMessage());
        verify(jobRepository, never()).delete(any(Job.class));
    }

    @Test
    void createJob_shouldSaveAndMap_whenValidInput() {
        Employer currentEmployer = buildEmployer(55L, "Scale Ltd");
        JobCreateUpdateDto dto = new JobCreateUpdateDto("Senior Java", "Build systems", 150_000d);

        Job saved = buildJob(888L, dto.getTitle(), dto.getDescription(), dto.getSalary(), currentEmployer);
        given(authService.getCurrentEmployer()).willReturn(currentEmployer);
        given(jobRepository.save(any(Job.class))).willReturn(saved);

        JobResponseDto result = jobService.createJob(dto);

        assertEquals(888L, result.getId());
        assertEquals("Senior Java", result.getTitle());
        assertEquals("Scale Ltd", result.getCompanyName());
        verify(authService).getCurrentEmployer();
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void createJob_shouldPropagateException_whenCurrentEmployerUnavailable() {
        JobCreateUpdateDto dto = new JobCreateUpdateDto("Senior Java", "Build systems", 150_000d);
        given(authService.getCurrentEmployer()).willThrow(new RuntimeException("Authenticated user not found"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> jobService.createJob(dto));

        assertEquals("Authenticated user not found", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void updateJob_shouldUpdateAndSave_whenJobExists() {
        Job existing = buildJob(11L, "Old", "Old desc", 10d, buildEmployer(70L, "ABC"));
        JobCreateUpdateDto dto = new JobCreateUpdateDto("New", "New desc", 20d);
        Job updated = buildJob(11L, dto.getTitle(), dto.getDescription(), dto.getSalary(), existing.getEmployer());

        given(jobRepository.findById(11L)).willReturn(Optional.of(existing));
        given(jobRepository.save(existing)).willReturn(updated);

        JobResponseDto result = jobService.updateJob(11L, dto);

        assertEquals("New", result.getTitle());
        assertEquals("New desc", result.getDescription());
        assertEquals(20d, result.getSalary());
        verify(jobRepository).findById(11L);
        verify(jobRepository).save(existing);
    }

    @Test
    void updateJob_shouldThrow_whenJobMissing() {
        given(jobRepository.findById(11L)).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> jobService.updateJob(11L, new JobCreateUpdateDto()));

        assertEquals("Job not found", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void createJob_shouldThrowNullPointerException_whenDtoIsNull() {
        Employer currentEmployer = buildEmployer(55L, "Scale Ltd");
        given(authService.getCurrentEmployer()).willReturn(currentEmployer);

        assertThrows(NullPointerException.class, () -> jobService.createJob(null));
        verify(jobRepository, never()).save(any(Job.class));
    }

    private static Job buildJob(Long id, String title, String description, Double salary, Employer employer) {
        Job job = new Job();
        job.setId(id);
        job.setTitle(title);
        job.setDescription(description);
        job.setSalary(salary);
        job.setEmployer(employer);
        return job;
    }

    private static Employer buildEmployer(Long id, String companyName) {
        Employer employer = new Employer();
        employer.setId(id);
        employer.setCompanyName(companyName);
        return employer;
    }
}