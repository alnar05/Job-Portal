package com.narek.jobportal.dto;

import com.narek.jobportal.entity.JobType;
import com.narek.jobportal.entity.SearchSortOption;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavedSearchDto {
    private Long id;
    private String name;
    private String keyword;
    private String location;
    private String companyName;
    private JobType jobType;
    private Double minSalary;
    private Double maxSalary;
    private SearchSortOption sortOption;
}
