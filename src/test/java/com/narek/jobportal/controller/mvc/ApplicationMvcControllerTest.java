package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.ApplicationResponseDto;
import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.entity.ApplicationStatus;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(ApplicationMvcController.class)
@Import(ApplicationMvcControllerTest.MvcSecurityConfig.class)
class ApplicationMvcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean(name = "authService")
    private AuthService authService;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private Validator validator;

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
    void applyForJob_shouldRedirectWithSuccess_whenValidInput() throws Exception {
        given(validator.validate(any(ApplicationCreateUpdateDto.class))).willReturn(Set.of());

        mockMvc.perform(post("/applications/apply/4")
                        .with(csrf())
                        .param("coverLetter", "I am interested"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs/4"))
                .andExpect(flash().attribute("successMessage", "Application submitted successfully."));

        verify(applicationService).createApplication(any(ApplicationCreateUpdateDto.class));
    }

    @Test
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void applyForJob_shouldRedirectWithError_whenValidationFails() throws Exception {
        ConstraintViolation<ApplicationCreateUpdateDto> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        given(violation.getMessage()).willReturn("Cover letter must be 200 words or fewer");
        given(validator.validate(any(ApplicationCreateUpdateDto.class))).willReturn(Set.of(violation));

        mockMvc.perform(post("/applications/apply/4")
                        .with(csrf())
                        .param("coverLetter", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs/4"))
                .andExpect(flash().attribute("errorMessage", "Cover letter must be 200 words or fewer"));
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYER"})
    void applyForJob_shouldReturnForbidden_whenWrongRole() throws Exception {
        mockMvc.perform(post("/applications/apply/4").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void myApplications_shouldRenderView_whenCandidate() throws Exception {
        Candidate candidate = new Candidate(3L, "Alice", "cv", null);
        given(authService.getCurrentCandidate()).willReturn(candidate);
        given(applicationService.getApplicationsByCandidateId(3L)).willReturn(List.of());

        mockMvc.perform(get("/applications/my"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/my-applications"))
                .andExpect(model().attributeExists("applications"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void applicationsForJob_shouldRenderView_whenAdmin() throws Exception {
        given(jobService.getJobById(2L)).willReturn(new JobResponseDto(2L, "Java", "Desc", 1000.0, com.narek.jobportal.entity.JobType.FULL_TIME, "Berlin", java.time.LocalDate.now().plusDays(10), "ACME"));
        given(applicationService.getApplicationsByJobId(2L)).willReturn(List.of());

        mockMvc.perform(get("/applications/job/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/job-applications"))
                .andExpect(model().attributeExists("job"))
                .andExpect(model().attributeExists("applications"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void applicationDetails_shouldRenderDetails_whenAdmin() throws Exception {
        ApplicationResponseDto details = new ApplicationResponseDto(5L, 1L, "Java", 4L, "Bob", "Cover", LocalDateTime.now(), ApplicationStatus.REVIEWED);
        given(applicationService.markAsReviewed(5L)).willReturn(details);

        mockMvc.perform(get("/applications/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/details"))
                .andExpect(model().attributeExists("applicationDetails"));
    }

    @Test
    @WithMockUser(username = "emp", roles = {"EMPLOYER"})
    void acceptApplication_shouldRedirectWithFlash_whenEmployerAuthorized() throws Exception {
        given(authService.isCurrentEmployerApplication(8L)).willReturn(true);

        mockMvc.perform(post("/applications/8/accept").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/8"))
                .andExpect(flash().attribute("successMessage", "Application accepted."));

        verify(applicationService).acceptApplication(8L);
    }

    @Test
    @WithMockUser(username = "cand", roles = {"CANDIDATE"})
    void acceptApplication_shouldReturnForbidden_whenWrongRole() throws Exception {
        mockMvc.perform(post("/applications/8/accept").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void rejectApplication_shouldRedirectWithFlash_whenAdmin() throws Exception {
        mockMvc.perform(post("/applications/8/reject").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/applications/8"))
                .andExpect(flash().attribute("successMessage", "Application rejected."));

        verify(applicationService).rejectApplication(8L);
    }
}