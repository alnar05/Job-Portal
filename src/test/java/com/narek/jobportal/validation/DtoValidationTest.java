package com.narek.jobportal.validation;

import com.narek.jobportal.dto.ApplicationCreateUpdateDto;
import com.narek.jobportal.dto.JobCreateUpdateDto;
import com.narek.jobportal.dto.RegistrationDto;
import com.narek.jobportal.entity.Role;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class DtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void givenNegativeSalary_whenValidateJobDto_thenSalaryViolationReturned() {
        JobCreateUpdateDto dto = new JobCreateUpdateDto("Java Dev", "Description", -1.0);

        Set<ConstraintViolation<JobCreateUpdateDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Salary must be greater than 0");
    }

    @Test
    void givenInvalidEmailAndBlankFields_whenValidateRegistrationDto_thenMultipleViolationsReturned() {
        RegistrationDto dto = new RegistrationDto();
        dto.setEmail("wrong-format");
        dto.setPassword("");
        dto.setConfirmPassword("");

        Set<ConstraintViolation<RegistrationDto>> violations = validator.validate(dto);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Please provide a valid email", "Password is required", "Role is required", "Passwords do not match");
    }

    @Test
    void givenEmployerRoleWithoutCompany_whenValidateRegistrationDto_thenCustomValidationFails() {
        RegistrationDto dto = new RegistrationDto();
        dto.setEmail("emp@mail.com");
        dto.setPassword("Password123");
        dto.setConfirmPassword("Password123");
        dto.setRole(Role.EMPLOYER);

        Set<ConstraintViolation<RegistrationDto>> violations = validator.validate(dto);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("Company name is required for employer registration");
    }

    @Test
    void givenCoverLetterOverWordLimit_whenValidateApplicationDto_thenWordCountViolationReturned() {
        String longCover = "word ".repeat(201);
        ApplicationCreateUpdateDto dto = new ApplicationCreateUpdateDto(1L, longCover);

        Set<ConstraintViolation<ApplicationCreateUpdateDto>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Cover letter must be 200 words or fewer");
    }
}