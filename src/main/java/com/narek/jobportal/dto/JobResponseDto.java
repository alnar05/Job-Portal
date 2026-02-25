package com.narek.jobportal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class JobResponseDto {

    private Long id;
    private String title;
    private String description;
    private Double salary;
    private String companyName;  // derived from Employer
}
