package com.narek.jobportal.specification;

import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.JobType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class JobSpecification {

    private JobSpecification() {
    }

    public static Specification<Job> notExpired() {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("closingDate"), LocalDate.now());
    }

    public static Specification<Job> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
            );
        };
    }

    public static Specification<Job> hasLocation(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + location.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("location")), like);
        };
    }

    public static Specification<Job> hasCompanyName(String companyName) {
        return (root, query, cb) -> {
            if (companyName == null || companyName.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + companyName.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("employer").get("companyName")), like);
        };
    }

    public static Specification<Job> hasJobType(JobType jobType) {
        return (root, query, cb) -> jobType == null
                ? cb.conjunction()
                : cb.equal(root.get("jobType"), jobType);
    }

    public static Specification<Job> overlapsSalaryRange(Double minSalary, Double maxSalary) {
        return (root, query, cb) -> {
            if (minSalary == null && maxSalary == null) {
                return cb.conjunction();
            }

            if (minSalary != null && maxSalary != null) {
                return cb.between(root.get("salary"), minSalary, maxSalary);
            }

            if (minSalary != null) {
                return cb.greaterThanOrEqualTo(root.get("salary"), minSalary);
            }

            return cb.lessThanOrEqualTo(root.get("salary"), maxSalary);
        };
    }
}
