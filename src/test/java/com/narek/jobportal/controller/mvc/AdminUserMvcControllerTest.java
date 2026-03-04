package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminUserMvcController.class)
@Import(AdminUserMvcControllerTest.TestSecurityConfig.class)
class AdminUserMvcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .formLogin(Customizer.withDefaults());
            return http.build();
        }
    }

    // GET /admin/users
    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_shouldRenderAdminUsersView_whenAdmin() throws Exception {
        User user = new User(1L, "employer@example.com", "secret", true, Set.of(Role.EMPLOYER), null, null);
        Page<User> users = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
        given(userService.getUsersByRole(Role.EMPLOYER, PageRequest.of(0, 10))).willReturn(users);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin-users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("selectedRole", Role.EMPLOYER));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void listUsers_shouldReturnForbidden_whenNonAdminRole() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUsersByRole(Role.EMPLOYER, PageRequest.of(0, 10));
    }

    @Test
    @WithAnonymousUser
    void listUsers_shouldRedirectToLogin_whenAnonymousUser() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }

    // GET /admin/users/{id}
    @Test
    @WithMockUser(roles = "ADMIN")
    void userDetails_shouldRenderAdminUserDetailsView_whenAdmin() throws Exception {
        User user = new User(5L, "candidate@example.com", "secret", true, Set.of(Role.CANDIDATE), null, null);
        given(userService.getUserById(5L)).willReturn(Optional.of(user));

        mockMvc.perform(get("/admin/users/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin-user-details"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", user));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void userDetails_shouldReturnForbidden_whenNonAdminRole() throws Exception {
        mockMvc.perform(get("/admin/users/5"))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(5L);
    }

    @Test
    @WithAnonymousUser
    void userDetails_shouldRedirectToLogin_whenAnonymousUser() throws Exception {
        mockMvc.perform(get("/admin/users/5"))
                .andExpect(status().is3xxRedirection());
    }

    // POST /admin/users/{id}/status
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_shouldRedirectToUserDetails_whenAdminEnablesUser() throws Exception {
        mockMvc.perform(post("/admin/users/7/status")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/7"))
                .andExpect(flash().attribute("successMessage", "User enabled."));

        verify(userService).setEnabled(7L, true);
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void updateStatus_shouldReturnForbidden_whenNonAdminRole() throws Exception {
        mockMvc.perform(post("/admin/users/7/status")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("enabled", "true"))
                .andExpect(status().isForbidden());

        verify(userService, never()).setEnabled(7L, true);
    }

    @Test
    @WithAnonymousUser
    void updateStatus_shouldRedirectToLogin_whenAnonymousUser() throws Exception {
        mockMvc.perform(post("/admin/users/7/status")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection());
    }

    // POST /admin/users/{id}/delete
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_shouldRedirectToUsersList_whenAdmin() throws Exception {
        mockMvc.perform(post("/admin/users/9/delete")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("successMessage", "User deleted."));

        verify(userService).deleteUser(9L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void deleteUser_shouldReturnForbidden_whenNonAdminRole() throws Exception {
        mockMvc.perform(post("/admin/users/9/delete")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(9L);
    }

    @Test
    @WithAnonymousUser
    void deleteUser_shouldRedirectToLogin_whenAnonymousUser() throws Exception {
        mockMvc.perform(post("/admin/users/9/delete")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection());
    }
}