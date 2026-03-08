package com.narek.jobportal;

import com.narek.jobportal.entity.*;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.CandidateRepository;
import com.narek.jobportal.repository.EmployerRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobPortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private EmployerRepository employerRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    void candidateFlow_shouldRegisterCandidateAndApplyForJob_andPersistApplication() throws Exception {
        registerEmployer("employer-flow@example.com", "Flow Employer Inc", "https://employer-flow.example");

        String createJobPayload = """
                {
                  "title": "Senior Java Engineer",
                  "description": "Build secure Spring Boot backend systems.",
                  "salary": 155000.0,
                  "jobType": "FULL_TIME",
                  "location": "Berlin",
                  "closingDate": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                        .with(user("employer-flow@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJobPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Senior Java Engineer"))
                .andExpect(jsonPath("$.companyName").value("Flow Employer Inc"));

        Job persistedJob = jobRepository.findAll().stream()
                .filter(job -> "Senior Java Engineer".equals(job.getTitle()))
                .findFirst()
                .orElseThrow();

        registerCandidate("candidate-flow@example.com", "Candidate Flow", "https://resume.example/candidate-flow");

        String applyPayload = """
                {
                  "jobId": %d,
                  "coverLetter": "I have five years of experience building resilient Java microservices."
                }
                """.formatted(persistedJob.getId());

        mockMvc.perform(post("/api/applications")
                        .with(user("candidate-flow@example.com").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(persistedJob.getId()))
                .andExpect(jsonPath("$.candidateName").value("candidate-flow@example.com"))
                .andExpect(jsonPath("$.status").value("APPLIED"));

        User registeredCandidate = userRepository.findByEmail("candidate-flow@example.com").orElseThrow();
        assertThat(registeredCandidate.getRoles()).containsExactly(Role.CANDIDATE);
        Long candidateId = candidateRepository.findByUserId(registeredCandidate.getId()).orElseThrow().getId();

        assertThat(applicationRepository.existsByJobIdAndCandidateId(persistedJob.getId(), candidateId)).isTrue();
    }

    @Test
    void employerFlow_shouldRegisterCreateUpdateRetrieveAndDeleteJob() throws Exception {
        registerEmployer("employer-e2e@example.com", "Acme Hiring", "https://acme.example");

        String createJobPayload = """
                {
                  "title": "Platform Engineer",
                  "description": "Maintain cloud-native recruitment platform.",
                  "salary": 140000.0,
                  "jobType": "FULL_TIME",
                  "location": "Berlin",
                  "closingDate": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                        .with(user("employer-e2e@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJobPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Platform Engineer"));

        User employerUser = userRepository.findByEmail("employer-e2e@example.com").orElseThrow();
        Long employerId = employerRepository.findByUserId(employerUser.getId()).orElseThrow().getId();
        Job createdJob = jobRepository.findByEmployerId(employerId).stream().findFirst().orElseThrow();

        String updateJobPayload = """
                {
                  "title": "Principal Platform Engineer",
                  "description": "Own architecture and delivery of hiring platform.",
                  "salary": 170000.0,
                  "jobType": "FULL_TIME",
                  "location": "Berlin",
                  "closingDate": "2099-12-31"
                }
                """;

        mockMvc.perform(put("/api/jobs/{id}", createdJob.getId())
                        .with(user("employer-e2e@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJobPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdJob.getId()))
                .andExpect(jsonPath("$.title").value("Principal Platform Engineer"));

        mockMvc.perform(get("/api/jobs/employer/{employerId}", employerId)
                        .with(user("employer-e2e@example.com").roles("EMPLOYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Principal Platform Engineer"))
                .andExpect(jsonPath("$[0].companyName").value("Acme Hiring"));

        mockMvc.perform(delete("/api/jobs/{id}", createdJob.getId())
                        .with(user("employer-e2e@example.com").roles("EMPLOYER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Job deleted successfully"));

        assertThat(jobRepository.findById(createdJob.getId())).isEmpty();
    }

    @Test
    void security_shouldRejectUnauthorizedAndForbiddenActions() throws Exception {
        registerEmployer("security-employer@example.com", "Secure Corp", "https://secure.example");

        String createJobPayload = """
                {
                  "title": "Security Engineer",
                  "description": "Protect APIs and data.",
                  "salary": 135000.0,
                  "jobType": "FULL_TIME",
                  "location": "Berlin",
                  "closingDate": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJobPayload))
                // Form-login apps redirect unauthenticated requests to /login (302).
                // httpBasic/stateless apps return 401. Both mean "not authenticated".
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(302, 401, 403));

        mockMvc.perform(post("/api/jobs")
                        .with(user("candidate-security@example.com").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJobPayload))
                .andExpect(status().isForbidden());

        // Create job properly as EMPLOYER (valid case)
        mockMvc.perform(post("/api/jobs")
                        .with(user("security-employer@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJobPayload))
                .andExpect(status().isOk());

        Long jobId = jobRepository.findAll().stream().findFirst().orElseThrow().getId();

        String applyPayload = """
                {
                  "jobId": %d,
                  "coverLetter": "I am trying to apply as employer and should be forbidden."
                }
                """.formatted(jobId);

        mockMvc.perform(post("/api/applications")
                        .with(user("security-employer@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void dataIntegrity_shouldDeleteRelatedApplications_whenJobIsDeleted() throws Exception {
        registerEmployer("cascade-employer@example.com", "Cascade Labs", "https://cascade.example");
        registerCandidate("cascade-candidate@example.com", "Cascade Candidate", "https://resume.example/cascade");

        String createJobPayload = """
                {
                  "title": "Backend Developer",
                  "description": "Build APIs that handle high traffic.",
                  "salary": 120000.0,
                  "jobType": "FULL_TIME",
                  "location": "Berlin",
                  "closingDate": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                        .with(user("cascade-employer@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJobPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Backend Developer"));

        Job job = jobRepository.findAll().stream().findFirst().orElseThrow();

        String applyPayload = """
                {
                  "jobId": %d,
                  "coverLetter": "I can contribute with production-grade Java and SQL skills."
                }
                """.formatted(job.getId());

        mockMvc.perform(post("/api/applications")
                        .with(user("cascade-candidate@example.com").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(job.getId()));

        assertThat(applicationRepository.findByJobId(job.getId())).hasSize(1);

        mockMvc.perform(delete("/api/jobs/{id}", job.getId())
                        .with(user("cascade-employer@example.com").roles("EMPLOYER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Job deleted successfully"));

        assertThat(jobRepository.findById(job.getId())).isEmpty();
        assertThat(applicationRepository.findByJobId(job.getId())).isEmpty();
        assertThat(applicationRepository.findAll())
                .extracting(Application::getJob)
                .noneMatch(existingJob -> existingJob.getId().equals(job.getId()));
    }

    @Test
    void employerCannotDeleteOtherEmployerJob() throws Exception {
        // Given
        registerEmployer("owner-employer@example.com", "Owner Corp", "https://owner.example");
        registerEmployer("other-employer@example.com", "Other Corp", "https://other.example");

        String createJobPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "title", "Owned Job",
                "description", "Job belongs to owner employer",
                "salary", 95000.0,
                "jobType", "FULL_TIME",
                "location", "Berlin",
                "closingDate", "2099-12-31"
        ));

        mockMvc.perform(post("/api/jobs")
                        .with(user("owner-employer@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJobPayload))
                .andExpect(status().isOk());

        User ownerUser = userRepository.findByEmail("owner-employer@example.com").orElseThrow();
        Long ownerEmployerId = employerRepository.findByUserId(ownerUser.getId()).orElseThrow().getId();
        Job createdJob = jobRepository.findByEmployerId(ownerEmployerId).stream().findFirst().orElseThrow();

        // When / Then
        mockMvc.perform(delete("/api/jobs/{id}", createdJob.getId())
                        .with(user("other-employer@example.com").roles("EMPLOYER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(jobRepository.findById(createdJob.getId())).isPresent();
    }

    @Test
    void candidateCannotApplyTwice() throws Exception {
        // Given
        registerEmployer("twice-employer@example.com", "Twice Hiring", "https://twice-hiring.example");
        registerCandidate("twice-candidate@example.com", "Twice Candidate", "https://resume.example/twice");

        String createJobPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "title", "API Developer",
                "description", "Build Java APIs",
                "salary", 110000.0,
                "jobType", "FULL_TIME",
                "location", "Berlin",
                "closingDate", "2099-12-31"
        ));

        mockMvc.perform(post("/api/jobs")
                        .with(user("twice-employer@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJobPayload))
                .andExpect(status().isOk());

        Job createdJob = jobRepository.findAll().stream()
                .filter(job -> "API Developer".equals(job.getTitle()))
                .findFirst()
                .orElseThrow();

        String applyPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "jobId", createdJob.getId(),
                "coverLetter", "I can contribute to API development"
        ));

        // When
        mockMvc.perform(post("/api/applications")
                        .with(user("twice-candidate@example.com").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyPayload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/applications")
                        .with(user("twice-candidate@example.com").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyPayload))
                .andExpect(status().isConflict());

        // Then
        User candidateUser = userRepository.findByEmail("twice-candidate@example.com").orElseThrow();
        Long candidateId = candidateRepository.findByUserId(candidateUser.getId()).orElseThrow().getId();
        long applicationCount = applicationRepository.findByJobId(createdJob.getId()).stream()
                .filter(application -> application.getCandidate().getId().equals(candidateId))
                .count();

        assertThat(applicationCount).isEqualTo(1L);
    }

    @Test
    void createJob_withInvalidPayload_returns400() throws Exception {
        // Given
        registerEmployer("invalid-job-employer@example.com", "Invalid Job Inc", "https://invalid-job.example");
        String invalidPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "title", "",
                "description", "",
                "salary", 0,
                "jobType", "FULL_TIME",
                "location", "",
                "closingDate", "2099-12-31"
        ));

        // When / Then
        mockMvc.perform(post("/api/jobs")
                        .with(user("invalid-job-employer@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }


    @Test
    void search_shouldFilterByCombinedCriteria_andExcludeExpiredJobs() throws Exception {
        registerEmployer("search-employer@example.com", "Search Corp", "https://search.example");

        String validJobPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "title", "Java Engineer",
                "description", "Spring and Java role",
                "salary", 5000.0,
                "jobType", "FULL_TIME",
                "location", "Berlin, Germany",
                "closingDate", "2099-12-31"
        ));

        String expiredJobPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "title", "Java Legacy",
                "description", "Legacy Java role",
                "salary", 5000.0,
                "jobType", "FULL_TIME",
                "location", "Berlin",
                "closingDate", "2000-01-01"
        ));

        mockMvc.perform(post("/api/jobs")
                        .with(user("search-employer@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJobPayload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/jobs")
                        .with(user("search-employer@example.com").roles("EMPLOYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expiredJobPayload))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/jobs/search")
                        .with(user("search-employer@example.com").roles("EMPLOYER"))
                        .param("query", "java")
                        .param("location", "berlin")
                        .param("jobType", "FULL_TIME")
                        .param("minSalary", "3000")
                        .param("maxSalary", "6000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Java Engineer"));
    }

    @Test
    void applying_afterClosingDate_shouldFail() throws Exception {
        registerEmployer("closed-employer@example.com", "Closed Corp", "https://closed.example");
        registerCandidate("closed-candidate@example.com", "Closed Candidate", "https://resume.example/closed");

        User employerUser = userRepository.findByEmail("closed-employer@example.com").orElseThrow();
        Long employerId = employerRepository.findByUserId(employerUser.getId()).orElseThrow().getId();

        Job closedJob = new Job();
        closedJob.setTitle("Expired Job");
        closedJob.setDescription("Old job");
        closedJob.setSalary(1000.0);
        closedJob.setJobType(JobType.FULL_TIME);
        closedJob.setLocation("Berlin");
        closedJob.setClosingDate(LocalDate.now().plusDays(5)); // valid
        closedJob.setEmployer(employerRepository.findById(employerId).orElseThrow());

        closedJob = jobRepository.save(closedJob);

        // force expiration after persistence
        closedJob.setClosingDate(LocalDate.now().minusDays(1));
        jobRepository.save(closedJob);

        String applyPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "jobId", closedJob.getId(),
                "coverLetter", "Trying to apply after close"
        ));

        mockMvc.perform(post("/api/applications")
                        .with(user("closed-candidate@example.com").roles("CANDIDATE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Job application period has closed"));
    }

    @PersistenceContext
    EntityManager em;

    @Test
    void expiredJobs_shouldNotAppearInSearchResults() throws Exception {
        Job job = createValidJob();

        // force expiration bypassing validation
        em.createQuery("UPDATE Job j SET j.closingDate = :date WHERE j.id = :id")
                .setParameter("date", LocalDate.now().minusDays(1))
                .setParameter("id", job.getId())
                .executeUpdate();

        mockMvc.perform(get("/api/jobs/search")
                        .with(user("search-candidate@example.com").roles("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void register_withExistingEmail_returnsConflict() throws Exception {
        // Given
        String email = "existing-register@example.com";

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", email)
                        .param("password", "Secret123")
                        .param("confirmPassword", "Secret123")
                        .param("role", Role.CANDIDATE.name())
                        .param("fullName", "Existing Candidate")
                        .param("resumeUrl", "https://resume.example/existing"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // When / Then
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", email)
                        .param("password", "Secret123")
                        .param("confirmPassword", "Secret123")
                        .param("role", Role.CANDIDATE.name())
                        .param("fullName", "Duplicate Candidate")
                        .param("resumeUrl", "https://resume.example/duplicate"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));

        long usersWithEmail = userRepository.findAll().stream()
                .filter(user -> email.equals(user.getEmail()))
                .count();

        assertThat(usersWithEmail).isEqualTo(1L);
    }

    private void registerCandidate(String email, String fullName, String resumeUrl) throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", email)
                        .param("password", "Secret123")
                        .param("confirmPassword", "Secret123")
                        .param("role", Role.CANDIDATE.name())
                        .param("fullName", fullName)
                        .param("resumeUrl", resumeUrl))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private void registerEmployer(String email, String companyName, String website) throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", email)
                        .param("password", "Secret123")
                        .param("confirmPassword", "Secret123")
                        .param("role", Role.EMPLOYER.name())
                        .param("companyName", companyName)
                        .param("website", website))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private Job createValidJob() {
        try {
            registerEmployer("test-employer@example.com", "Test Corp", "https://test.example");
        } catch (Exception e) {
        }

        User employerUser = userRepository.findByEmail("test-employer@example.com")
                .orElseThrow();
        Long employerId = employerRepository.findByUserId(employerUser.getId())
                .orElseThrow()
                .getId();
        Employer employer = employerRepository.findById(employerId)
                .orElseThrow();

        Job job = new Job();
        job.setTitle("Backend Developer");
        job.setDescription("Spring Boot job for integration testing");
        job.setSalary(2000.0);
        job.setJobType(JobType.FULL_TIME);
        job.setLocation("Berlin");
        job.setClosingDate(LocalDate.now().plusDays(5)); // valid future date
        job.setEmployer(employer);

        return jobRepository.save(job);
    }
}