package com.narek.jobportal.dto;

import com.narek.jobportal.entity.ApplicationStatus;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminApplicationFilterDto {
    private Long candidateId;
    private Long jobId;
    private ApplicationStatus status;
    @Min(0)
    private Integer page = 0;
    @Min(1)
    private Integer size = 10;
}
