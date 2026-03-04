package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.RegistrationDto;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RegistrationController.class)
@Import(RegistrationControllerTest.MvcSecurityConfig.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;


    @TestConfiguration
    @EnableMethodSecurity
    static class MvcSecurityConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .formLogin(Customizer.withDefaults())
                    .csrf(Customizer.withDefaults());
            return http.build();
        }
    }

    @Test
    @WithAnonymousUser
    void showRegistrationForm_shouldRenderRegisterView_whenAnonymous() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registrationForm"));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void showRegistrationForm_shouldReturnForbidden_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void register_shouldRedirectToLogin_whenValidInput() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "new@site.com")
                        .param("password", "secret1")
                        .param("confirmPassword", "secret1")
                        .param("role", Role.CANDIDATE.name())
                        .param("fullName", "Alice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage",
                        "Registration successful. Please sign in."));

        verify(registrationService).register(any(RegistrationDto.class));
    }

    @Test
    @WithAnonymousUser
    void register_shouldReturnRegisterView_whenInvalidInput() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "bad-email")
                        .param("password", "123")
                        .param("confirmPassword", "1")
                        .param("role", Role.CANDIDATE.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @WithAnonymousUser
    void register_shouldReturnRegisterView_whenEmailConflict() throws Exception {
        willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use"))
                .given(registrationService).register(any(RegistrationDto.class));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "existing@site.com")
                        .param("password", "secret1")
                        .param("confirmPassword", "secret1")
                        .param("role", Role.CANDIDATE.name())
                        .param("fullName", "Alice"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void register_shouldReturnForbidden_whenAuthenticated() throws Exception {
        mockMvc.perform(post("/register").with(csrf()))
                .andExpect(status().isForbidden());
    }
}