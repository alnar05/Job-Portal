package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import com.narek.jobportal.service.UserService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.context.bean.override.mockito.MockReset.BEFORE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityMvcTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean(reset = BEFORE)
    private UserService userService;

    @BeforeEach
    void setUpUsers() {
        userRepository.deleteAll();

        User candidate = new User();
        candidate.setEmail("candidate@mail.com");
        candidate.setPassword(passwordEncoder.encode("Password123"));
        candidate.setEnabled(true);
        candidate.setRoles(Set.of(Role.CANDIDATE));

        User disabled = new User();
        disabled.setEmail("disabled@mail.com");
        disabled.setPassword(passwordEncoder.encode("Password123"));
        disabled.setEnabled(false);
        disabled.setRoles(Set.of(Role.CANDIDATE));

        User employer = new User();
        employer.setEmail("employer@mail.com");
        employer.setPassword(passwordEncoder.encode("Password123"));
        employer.setEnabled(true);
        employer.setRoles(Set.of(Role.EMPLOYER));

        userRepository.saveAll(List.of(candidate, disabled, employer));

        when(userService.getUsersByRole(any(), any())).thenReturn(new PageImpl<>(List.of()));
        doNothing().when(userService).setEnabled(any(), any(Boolean.class));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void givenCandidateRole_whenAccessAdminEndpoints_thenForbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void givenEmployerRole_whenAccessCandidateEndpoints_thenForbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/dashboard/candidate"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenDisabledUser_whenLogin_thenRedirectWithDisabledErrorCode() throws Exception {
        mockMvc.perform(formLogin().user("disabled@mail.com").password("Password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=disabled"));
    }

    @Test
    void givenEmployerCredentials_whenLogin_thenRedirectToDashboard() throws Exception {
        mockMvc.perform(formLogin().user("employer@mail.com").password("Password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminPostStatusUpdate_whenSubmit_thenUsesPrgRedirect() throws Exception {
        mockMvc.perform(post("/admin/users/99/status").param("enabled", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/99"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenPostWithoutCsrfToken_whenCsrfDisabled_thenRequestStillRedirects() throws Exception {
        mockMvc.perform(post("/admin/users/99/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }
}