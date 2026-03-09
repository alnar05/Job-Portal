package com.narek.jobportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "jobs")
@Check(constraints = "salary > 0")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    private String title;

    @Column(nullable = false, length = 4000)
    @NotBlank(message = "Description is required")
    private String description;

    @Column(nullable = false)
    @Positive(message = "Salary must be greater than 0")
    private Double salary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Job type is required")
    private JobType jobType;

    @NotBlank(message = "Location is required")
    @Column(nullable = false)
    private String location;

    @Future(message = "Closing date must be in the future")
    @NotNull(message = "Closing date is required")
    @Column(nullable = false)
    private LocalDate closingDate;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private Employer employer;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Application> applications = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(closingDate);
    }

    public boolean isActive() {
        return status == JobStatus.ACTIVE && !isExpired();
    }
}
