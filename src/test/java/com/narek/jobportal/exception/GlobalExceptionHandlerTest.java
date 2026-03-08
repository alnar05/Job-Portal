package com.narek.jobportal.exception;

import com.narek.jobportal.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void entityNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/jobs/9999")
                        .with(user("candidate@example.com").roles("CANDIDATE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void validationError_returns400() throws Exception {
        String invalidPayload = """
            {
              "title": "",
              "description": "",
              "salary": -100,
              "jobType": "FULL_TIME",
              "location": "",
              "closingDate": "2099-12-31"
            }
            """;

        mockMvc.perform(post("/api/jobs")
                        .with(user("employer@example.com").roles("EMPLOYER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // You can add more tests for:
    // - AccessDeniedException → 403
    // - generic Exception → 500
}