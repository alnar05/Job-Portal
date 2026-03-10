package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.dto.AdminJobFilterDto;
import com.narek.jobportal.entity.ApplicationStatus;
import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.JobStatus;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DashboardController.class)
@Import(DashboardControllerTest.MvcSecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private UserService userService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MvcSecurityConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .formLogin(Customizer.withDefaults());
            return http.build();
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void dashboard_shouldRedirectToAdminDashboard_whenAdmin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"));
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYER"})
    void dashboard_shouldRedirectToEmployerDashboard_whenEmployer() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/employer"));
    }

    @Test
    void dashboard_shouldRedirectToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminDashboard_shouldRenderView_whenAdmin() throws Exception {
        User employerUser = new User(1L, "emp@site", "pw", true, Set.of(Role.EMPLOYER), null, null);
        User candidateUser = new User(2L, "cand@site", "pw", true, Set.of(Role.CANDIDATE), null, null);

        Job activeJob = new Job();
        activeJob.setTitle("Java Dev");
        activeJob.setStatus(JobStatus.ACTIVE);
        activeJob.setClosingDate(LocalDate.now().plusDays(2));

        Job closedJob = new Job();
        closedJob.setTitle("Python Dev");
        closedJob.setStatus(JobStatus.CLOSED);
        closedJob.setClosingDate(LocalDate.now().plusDays(10));

        given(jobService.searchAdminJobs(any(AdminJobFilterDto.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(activeJob, closedJob)));
        given(jobService.getRecentJobs(5)).willReturn(List.of());
        given(applicationService.getAllApplications())
                .willReturn(List.of(new ApplicationResponseDto(1L, 1L, "Java Dev", 1L, "cand@site", null, LocalDateTime.now(), ApplicationStatus.APPLIED)));
        given(applicationService.getRecentApplications(5)).willReturn(List.of());
        given(userService.getRecentUsers(5)).willReturn(List.of());
        given(userService.getUsersByRole(Role.EMPLOYER, PageRequest.of(0, 10_000))).willReturn(new PageImpl<>(List.of(employerUser)));
        given(userService.getUsersByRole(Role.CANDIDATE, PageRequest.of(0, 10_000))).willReturn(new PageImpl<>(List.of(candidateUser)));

        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin"))
                .andExpect(model().attributeExists("totalJobs", "totalApplications", "employerUsers", "candidateUsers"));
    }

    @Test
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void adminDashboard_shouldReturnForbidden_whenWrongRole() throws Exception {
        mockMvc.perform(get("/dashboard/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYER"})
    void employerDashboard_shouldRenderView_whenEmployer() throws Exception {
        given(authService.getCurrentEmployer()).willReturn(new Employer(3L, "ACME", "acme.com", null));
        given(jobService.getJobsByEmployerId(3L)).willReturn(List.of());

        mockMvc.perform(get("/dashboard/employer"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/employer"))
                .andExpect(model().attributeExists("jobs"));
    }

    @Test
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void candidateDashboard_shouldRenderView_whenCandidate() throws Exception {
        given(authService.getCurrentCandidate()).willReturn(new Candidate(4L, "Bob", "cv", null));
        given(applicationService.getApplicationsByCandidateId(4L)).willReturn(List.of());

        mockMvc.perform(get("/dashboard/candidate"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/candidate"))
                .andExpect(model().attributeExists("applications"));
    }
}