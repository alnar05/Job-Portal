package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
class RegistrationControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegistrationService registrationService;

    @Test
    @WithAnonymousUser
    void givenInvalidPayload_whenRegister_thenStayOnRegisterWithErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", "bad-email")
                        .param("password", "123")
                        .param("confirmPassword", "456"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasErrors("registrationForm"));
    }

    @Test
    @WithAnonymousUser
    void givenExistingEmail_whenRegister_thenShowFieldError() throws Exception {
        doThrow(new ResponseStatusException(CONFLICT, "Email is already registered"))
                .when(registrationService).register(any());

        mockMvc.perform(post("/register")
                        .param("email", "user@mail.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .param("role", "CANDIDATE")
                        .param("fullName", "Name"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));
    }

    @Test
    @WithAnonymousUser
    void givenValidForm_whenRegister_thenRedirectToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", "user@mail.com")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123")
                        .param("role", "CANDIDATE")
                        .param("fullName", "Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser
    void givenAuthenticatedUser_whenAccessRegister_thenForbidden() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isForbidden());
    }
}