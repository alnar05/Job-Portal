package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.service.ApplicationService;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import com.narek.jobportal.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private JobService jobService;
    @MockitoBean private ApplicationService applicationService;
    @MockitoBean private UserService userService;

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void givenCandidateRole_whenOpenDashboard_thenRedirectToCandidateDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/candidate"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void givenEmployerRole_whenOpenDashboard_thenRedirectToEmployerDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/employer"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_whenOpenDashboard_thenRedirectToAdminDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/admin"));
    }
}