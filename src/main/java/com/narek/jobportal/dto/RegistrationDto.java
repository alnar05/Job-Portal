package com.narek.jobportal.dto;

import com.narek.jobportal.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistrationDto {

    @Email(message = "Please provide a valid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotNull(message = "Role is required")
    private Role role;

    private String companyName;

    private String website;

    private String fullName;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    @AssertTrue(message = "Company name is required for employer registration")
    public boolean isCompanyNameValidForEmployer() {
        if (role != Role.EMPLOYER) {
            return true;
        }
        return companyName != null && !companyName.isBlank();
    }

    @AssertTrue(message = "Full name is required for candidate registration")
    public boolean isFullNameValidForCandidate() {
        if (role != Role.CANDIDATE) {
            return true;
        }
        return fullName != null && !fullName.isBlank();
    }
}
