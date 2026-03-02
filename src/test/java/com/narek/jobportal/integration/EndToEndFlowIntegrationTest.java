package com.narek.jobportal.integration;

import com.narek.jobportal.entity.*;
import com.narek.jobportal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EndToEndFlowIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CandidateRepository candidateRepository;
    @Autowired private EmployerRepository employerRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Candidate candidate;
    private Employer employer;
    private Job job;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        jobRepository.deleteAll();
        candidateRepository.deleteAll();
        employerRepository.deleteAll();
        userRepository.deleteAll();

        User candidateUser = new User();
        candidateUser.setEmail("candidate-flow@mail.com");
        candidateUser.setPassword(passwordEncoder.encode("Password123"));
        candidateUser.setEnabled(true);
        candidateUser.setRoles(Set.of(Role.CANDIDATE));
        candidateUser = userRepository.save(candidateUser);

        candidate = new Candidate();
        candidate.setUser(candidateUser);
        candidate.setFullName("Flow Candidate");
        candidate.setResumeUrl("https://resume.example/flow");
        candidate = candidateRepository.save(candidate);

        User employerUser = new User();
        employerUser.setEmail("employer-flow@mail.com");
        employerUser.setPassword(passwordEncoder.encode("Password123"));
        employerUser.setEnabled(true);
        employerUser.setRoles(Set.of(Role.EMPLOYER));
        employerUser = userRepository.save(employerUser);

        employer = new Employer();
        employer.setUser(employerUser);
        employer.setCompanyName("Flow Corp");
        employer.setWebsite("https://flow.example");
        employer = employerRepository.save(employer);

        job = new Job();
        job.setTitle("Backend Engineer");
        job.setDescription("Flow test job");
        job.setSalary(5000.0);
        job.setEmployer(employer);
        job = jobRepository.save(job);

        User adminUser = new User();
        adminUser.setEmail("admin-flow@mail.com");
        adminUser.setPassword(passwordEncoder.encode("Password123"));
        adminUser.setEnabled(true);
        adminUser.setRoles(Set.of(Role.ADMIN));
        userRepository.save(adminUser);
    }

    @Test
    void flow1_candidateAppliesAndEmployerAccepts_thenStatusBecomesAccepted() throws Exception {
        MvcResult candidateLogin = mockMvc.perform(formLogin().user("candidate-flow@mail.com").password("Password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mockMvc.perform(post("/applications/apply/{jobId}", job.getId())
                        .session((org.springframework.mock.web.MockHttpSession) candidateLogin.getRequest().getSession(false))
                        .param("coverLetter", "Flow application"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs/" + job.getId()));

        Application application = applicationRepository.findByCandidateId(candidate.getId()).stream().findFirst().orElseThrow();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);

        MvcResult employerLogin = mockMvc.perform(formLogin().user("employer-flow@mail.com").password("Password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mockMvc.perform(post("/applications/{id}/accept", application.getId())
                        .session((org.springframework.mock.web.MockHttpSession) employerLogin.getRequest().getSession(false)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/" + application.getId()));

        Application updated = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    void flow2_adminDisablesCandidate_thenCandidateLoginFailsWithDisabledError() throws Exception {
        User persistedCandidateUser = userRepository.findByEmail("candidate-flow@mail.com").orElseThrow();

        MvcResult adminLogin = mockMvc.perform(formLogin().user("admin-flow@mail.com").password("Password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mockMvc.perform(post("/admin/users/{id}/status", persistedCandidateUser.getId())
                        .session((org.springframework.mock.web.MockHttpSession) adminLogin.getRequest().getSession(false))
                        .param("enabled", "false"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(formLogin().user("candidate-flow@mail.com").password("Password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=disabled"));

        User disabledCandidate = userRepository.findByEmail("candidate-flow@mail.com").orElseThrow();
        assertThat(disabledCandidate.isEnabled()).isFalse();
    }
}