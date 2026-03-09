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

    public JobResponseDto(Long id,
                          String title,
                          String description,
                          Double salary,
                          JobType jobType,
                          String location,
                          LocalDate postedDate,
                          String companyName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.salary = salary;
        this.jobType = jobType;
        this.location = location;
        this.closingDate = postedDate;
        this.companyName = companyName;
        this.status = null;
        this.viewCount = null;
        this.applicationCount = null;
    }
}
