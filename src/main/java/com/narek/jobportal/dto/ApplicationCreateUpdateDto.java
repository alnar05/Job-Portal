package com.narek.jobportal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateUpdateDto {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private String coverLetter;
}
