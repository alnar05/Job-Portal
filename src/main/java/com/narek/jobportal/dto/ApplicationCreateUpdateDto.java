package com.narek.jobportal.dto;

import com.narek.jobportal.validation.WordCount;
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

    @WordCount(max = 200, message = "Cover letter must be 200 words or fewer")
    private String coverLetter;
}
