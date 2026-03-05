package com.narek.jobportal;

import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.CandidateRepository;
import com.narek.jobportal.repository.EmployerRepository;
import com.narek.jobportal.repository.JobRepository;
import com.narek.jobportal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobPortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
                  "salary": 155000.0
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
                  "salary": 140000.0
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
                  "salary": 170000.0
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
                  "salary": 135000.0
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
                  "salary": 120000.0
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
}