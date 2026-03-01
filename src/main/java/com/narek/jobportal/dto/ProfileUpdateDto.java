package com.narek.jobportal.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileUpdateDto {

    @Size(max = 255, message = "Full name must be up to 255 characters")
    private String fullName;

    @Size(max = 255, message = "Resume URL must be up to 255 characters")
    private String resumeUrl;

    @Size(max = 255, message = "Company name must be up to 255 characters")
    private String companyName;

    @Size(max = 255, message = "Website must be up to 255 characters")
    private String website;
}