package com.narek.jobportal.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkApplicationActionDto {
    @NotEmpty
    private List<Long> applicationIds;

    @NotNull
    private Boolean accept;

    private String internalNotes;
}
