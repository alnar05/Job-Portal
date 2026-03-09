package com.narek.jobportal.dto;

import com.narek.jobportal.entity.JobStatus;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminJobFilterDto {
    private JobStatus status;
    private Double minSalary;
    private Double maxSalary;
    private Long employerId;
    private String sortBy = "createdAt";
    private String direction = "desc";
    @Min(0)
    private Integer page = 0;
    @Min(1)
    private Integer size = 10;
}
