package com.narek.jobportal.controller.mvc;

import com.narek.jobportal.dto.JobResponseDto;
import com.narek.jobportal.service.AuthService;
import com.narek.jobportal.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobMvcController.class)
class JobMvcControllerWebMvcTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JobService jobService;
    @MockitoBean private AuthService authService;

    @Test
    void givenAnonymous_whenAccessJobs_thenUnauthorizedRedirectToLogin() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void givenInvalidJobForm_whenCreateJob_thenReturnFormView() throws Exception {
        mockMvc.perform(post("/jobs/create")
                        .param("title", "")
                        .param("description", "desc")
                        .param("salary", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/create"))
                .andExpect(model().attributeHasFieldErrors("jobForm", "title", "salary"));
    }

    @Test
    @WithMockUser(roles = "CANDIDATE")
    void givenWrongRole_whenCreateJob_thenForbidden() throws Exception {
        mockMvc.perform(post("/jobs/create")
                        .param("title", "Java Dev")
                        .param("description", "desc")
                        .param("salary", "1000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void givenValidForm_whenCreateJob_thenRedirectPrg() throws Exception {
        mockMvc.perform(post("/jobs/create")
                        .param("title", "Java Dev")
                        .param("description", "desc")
                        .param("salary", "1000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/jobs"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void givenEmployerDeleteJob_whenDelete_thenRedirectToEmployerDashboard() throws Exception {
        mockMvc.perform(post("/jobs/delete/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employer/dashboard"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYER")
    void givenJobDetails_whenGetById_thenRenderDetails() throws Exception {
        JobResponseDto dto = new JobResponseDto();
        dto.setId(1L);
        dto.setTitle("Title");
        when(jobService.getJobById(1L)).thenReturn(dto);

        mockMvc.perform(get("/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/details"))
                .andExpect(model().attributeExists("job"));
    }
}