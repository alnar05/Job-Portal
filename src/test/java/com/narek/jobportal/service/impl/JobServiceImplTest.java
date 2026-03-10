package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.JobStatus;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.service.AuthService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        given(jobRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).willReturn(List.of(first, second));

        List<JobResponseDto> result = jobService.getAllJobs();

        assertEquals(2, result.size());
        assertEquals("Java Engineer", result.get(0).getTitle());
        assertEquals("Globex", result.get(1).getCompanyName());
    }

    @Test
    void searchJobs_shouldReturnPage() {
        Job job = buildJob(5L, "Java Backend", "API role", 120_000d, buildEmployer(20L, "Tech Corp"));
        given(jobRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(job)));

        Page<JobResponseDto> page = jobService.searchJobs("java", "Berlin", JobType.FULL_TIME, 1000d, 200000d, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Java Backend", page.getContent().getFirst().getTitle());
    }

    @Test
    void getJobById_shouldThrowException_whenJobMissing() {
        given(jobRepository.findById(99L)).willReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> jobService.getJobById(99L));

        assertEquals("Job not found with id 99", exception.getMessage());
    }

    @Test
    void createJob_shouldSaveAndMap_whenValidInput() {
        Employer currentEmployer = buildEmployer(55L, "Scale Ltd");
        JobCreateUpdateDto dto = new JobCreateUpdateDto("Senior Java", "Build systems", 150_000d, JobType.FULL_TIME, "Berlin", LocalDate.now().plusDays(10));

        Job saved = buildJob(888L, dto.getTitle(), dto.getDescription(), dto.getSalary(), currentEmployer);
        given(authService.getCurrentEmployer()).willReturn(currentEmployer);
        given(jobRepository.save(any(Job.class))).willReturn(saved);

        JobResponseDto result = jobService.createJob(dto);

        assertEquals(888L, result.getId());
        assertEquals("Senior Java", result.getTitle());
        assertEquals("Scale Ltd", result.getCompanyName());
    }

    @Test
    void updateJob_shouldUpdateAndSave_whenJobExists() {
        Job existing = buildJob(11L, "Old", "Old desc", 10d, buildEmployer(70L, "ABC"));
        JobCreateUpdateDto dto = new JobCreateUpdateDto("New", "New desc", 20d, JobType.CONTRACT, "Yerevan", LocalDate.now().plusDays(5));
        Job updated = buildJob(11L, dto.getTitle(), dto.getDescription(), dto.getSalary(), existing.getEmployer());

        given(jobRepository.findById(11L)).willReturn(Optional.of(existing));
        given(jobRepository.save(existing)).willReturn(updated);

        JobResponseDto result = jobService.updateJob(11L, dto);

        assertEquals("New", result.getTitle());
        assertEquals("New desc", result.getDescription());
        assertEquals(20d, result.getSalary());
        verify(jobRepository).save(existing);
    }

    @Test
    void updateJob_shouldRejectEmployerChanges_whenExpired() {
        Job existing = buildJob(11L, "Old", "Old desc", 10d, buildEmployer(70L, "ABC"));
        existing.setClosingDate(LocalDate.now().minusDays(1));
        existing.setStatus(JobStatus.ACTIVE);
        JobCreateUpdateDto dto = new JobCreateUpdateDto("New", "New desc", 20d, JobType.CONTRACT, "Yerevan", LocalDate.now().plusDays(5));

        given(jobRepository.findById(11L)).willReturn(Optional.of(existing));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> jobService.updateJob(11L, dto));

        assertTrue(exception.getMessage().contains("Expired jobs cannot be modified"));
    }

    @Test
    void updateJob_shouldThrow_whenJobMissing() {
        given(jobRepository.findById(11L)).willReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> jobService.updateJob(11L, new JobCreateUpdateDto()));

        assertEquals("Job not found with id 11", exception.getMessage());
        verify(jobRepository, never()).save(any(Job.class));
    }

    private static Job buildJob(Long id, String title, String description, Double salary, Employer employer) {
        Job job = new Job();
        job.setId(id);
        job.setTitle(title);
        job.setDescription(description);
        job.setSalary(salary);
        job.setEmployer(employer);
        job.setJobType(JobType.FULL_TIME);
        job.setLocation("Berlin");
        job.setClosingDate(LocalDate.now().plusDays(20));
        return job;
    }

    private static Employer buildEmployer(Long id, String companyName) {
        Employer employer = new Employer();
        employer.setId(id);
        employer.setCompanyName(companyName);
        return employer;
    }
}
