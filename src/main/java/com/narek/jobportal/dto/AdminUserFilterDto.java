package com.narek.jobportal.dto;

import com.narek.jobportal.entity.Role;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AdminUserFilterDto {
    private Role role;
    private Boolean enabled;
    private String email;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registeredFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registeredTo;
    @Min(0)
    private Integer page = 0;
    @Min(1)
    private Integer size = 10;
}
