package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(JobMvcController.class)
@Import(JobMvcControllerTest.MvcSecurityConfig.class)
class JobMvcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean(name = "authService")
    private AuthService authService;

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
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void listJobs_shouldRenderJobsList_whenAuthenticated() throws Exception {
        JobResponseDto job = new JobResponseDto(1L, "Java", "Desc", 100.0, JobType.FULL_TIME, "Berlin", LocalDate.now().plusDays(10), "ACME");
        given(jobService.searchJobs(eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(job), PageRequest.of(0, 9), 1));

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/list"))
                .andExpect(model().attributeExists("jobs"))
                .andExpect(model().attribute("keyword", (Object) null))
                .andExpect(model().attribute("selectedJobType", (Object) null));
    }

    @Test
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void listJobs_shouldApplyFilters_whenQueryParametersProvided() throws Exception {
        given(jobService.searchJobs(eq("java"), eq("Berlin"), eq(JobType.FULL_TIME), eq(1000.0), eq(5000.0), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));

        mockMvc.perform(get("/jobs")
                        .param("keyword", "java")
                        .param("location", "Berlin")
                        .param("jobType", "FULL_TIME")
                        .param("minSalary", "1000")
                        .param("maxSalary", "5000"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/list"))
                .andExpect(model().attribute("keyword", "java"))
                .andExpect(model().attribute("location", "Berlin"))
                .andExpect(model().attribute("selectedJobType", JobType.FULL_TIME))
                .andExpect(model().attribute("minSalary", 1000.0))
                .andExpect(model().attribute("maxSalary", 5000.0));
    }

    @Test
    void listJobs_shouldRedirectToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void jobDetails_shouldRenderDetails_whenAuthenticated() throws Exception {
        given(jobService.getJobById(2L)).willReturn(new JobResponseDto(2L, "QA", "Tests", 90.0, com.narek.jobportal.entity.JobType.CONTRACT, "Paris", LocalDate.now().plusDays(7), "Globex"));

        mockMvc.perform(get("/jobs/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/details"))
                .andExpect(model().attributeExists("job"));
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYER"})
    void createJob_shouldRedirectToJobs_whenEmployerAndValidInput() throws Exception {
        mockMvc.perform(post("/jobs/create")
                        .with(csrf())
                        .param("title", "Java")
                        .param("description", "Backend")
                        .param("salary", "1000")
                        .param("jobType", "FULL_TIME")
                        .param("location", "Berlin")
                        .param("closingDate", "2099-12-31"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"))
                .andExpect(flash().attribute("successMessage", "Job posted successfully."));

        verify(jobService).createJob(any(JobCreateUpdateDto.class));
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYER"})
    void createJob_shouldReturnCreateView_whenInvalidInput() throws Exception {
        mockMvc.perform(post("/jobs/create")
                        .with(csrf())
                        .param("title", "")
                        .param("description", "")
                        .param("salary", "")
                        .param("jobType", "")
                        .param("location", "")
                        .param("closingDate", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/create"));
    }

    @Test
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void createJob_shouldReturnForbidden_whenWrongRole() throws Exception {
        mockMvc.perform(post("/jobs/create")
                        .with(csrf())
                        .param("title", "Java")
                        .param("description", "Backend")
                        .param("salary", "1000")
                        .param("jobType", "FULL_TIME")
                        .param("location", "Berlin")
                        .param("closingDate", "2099-12-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateJob_shouldRedirectToDetails_whenAdminAndValidInput() throws Exception {
        mockMvc.perform(post("/jobs/edit/8")
                        .with(csrf())
                        .param("title", "Updated")
                        .param("description", "Updated desc")
                        .param("salary", "5000")
                        .param("jobType", "CONTRACT")
                        .param("location", "Yerevan")
                        .param("closingDate", "2099-12-31"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs/8"))
                .andExpect(flash().attribute("successMessage", "Job updated successfully."));

        verify(jobService).updateJob(any(Long.class), any(JobCreateUpdateDto.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateJob_shouldReturnEditView_whenInvalidInput() throws Exception {
        mockMvc.perform(post("/jobs/edit/8")
                        .with(csrf())
                        .param("title", "")
                        .param("description", "")
                        .param("salary", "")
                        .param("jobType", "")
                        .param("location", "")
                        .param("closingDate", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/edit"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteJob_shouldRedirectToJobs_whenAdmin() throws Exception {
        mockMvc.perform(post("/jobs/delete/9").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));

        verify(jobService).deleteJob(9L);
    }
}
