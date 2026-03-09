package com.narek.jobportal.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChartPointDto {
    private String label;
    private long value;
}
