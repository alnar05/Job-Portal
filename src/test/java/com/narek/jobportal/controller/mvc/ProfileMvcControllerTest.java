package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.ProfileUpdateDto;
import com.narek.jobportal.entity.Candidate;
import com.narek.jobportal.entity.Employer;
import com.narek.jobportal.service.CandidateService;
import com.narek.jobportal.service.EmployerService;
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

import java.util.Optional;

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

@WebMvcTest(ProfileMvcController.class)
@Import(ProfileMvcControllerTest.MvcSecurityConfig.class)
class ProfileMvcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateService candidateService;

    @MockitoBean
    private EmployerService employerService;

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
    @WithMockUser(username = "emp@site.com", roles = {"EMPLOYER"})
    void editProfile_shouldRenderEditView_whenEmployer() throws Exception {
        given(employerService.getEmployerByUserEmail("emp@site.com"))
                .willReturn(Optional.of(new Employer(1L, "ACME", "acme.com", null)));

        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("profileForm"))
                .andExpect(model().attribute("isEmployer", true));
    }

    @Test
    @WithMockUser(username = "cand@site.com", roles = {"CANDIDATE"})
    void editProfile_shouldRenderEditView_whenCandidate() throws Exception {
        given(candidateService.getCandidateByUserEmail("cand@site.com"))
                .willReturn(Optional.of(new Candidate(2L, "Alice", "resume.pdf", null)));

        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attribute("isEmployer", false));
    }

    @Test
    void editProfile_shouldRedirectToLogin_whenAnonymous() throws Exception {
        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "emp@site.com", roles = {"EMPLOYER"})
    void updateProfile_shouldRedirectToEmployerDashboard_whenEmployerAndValidInput() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("companyName", "NewCo")
                        .param("website", "new.co"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/employer"))
                .andExpect(flash().attribute("successMessage", "Employer profile updated successfully."));

        verify(employerService).updateOwnProfile(any(ProfileUpdateDto.class), eq("emp@site.com"));
    }

    @Test
    @WithMockUser(username = "cand@site.com", roles = {"CANDIDATE"})
    void updateProfile_shouldRedirectToCandidateDashboard_whenCandidateAndValidInput() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "Alice")
                        .param("resumeUrl", "resume.pdf"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/candidate"))
                .andExpect(flash().attribute("successMessage", "Candidate profile updated successfully."));

        verify(candidateService).updateOwnProfile(any(ProfileUpdateDto.class), eq("cand@site.com"));
    }

    @Test
    @WithMockUser(username = "cand@site.com", roles = {"CANDIDATE"})
    void updateProfile_shouldReturnEditView_whenInvalidInput() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "x".repeat(256)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));
    }
}
