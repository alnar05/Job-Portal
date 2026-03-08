package com.narek.jobportal.dto;

import com.narek.jobportal.entity.JobStatus;
import com.narek.jobportal.entity.JobType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class JobResponseDto {

    private Long id;
    private String title;
    private String description;
    private Double salary;
    private JobType jobType;
    private String location;
    private LocalDate closingDate;
    private String companyName;
    private JobStatus status;
    private Long viewCount;
    private Long applicationCount;
}
