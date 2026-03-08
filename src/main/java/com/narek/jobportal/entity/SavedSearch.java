package com.narek.jobportal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class SavedSearch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Candidate candidate;

    @Column(nullable = false)
    private String name;

    private String keyword;
    private String location;
    private String companyName;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    private Double minSalary;
    private Double maxSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SearchSortOption sortOption = SearchSortOption.NEWEST;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
