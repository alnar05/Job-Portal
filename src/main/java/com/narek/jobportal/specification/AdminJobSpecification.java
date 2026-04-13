package com.narek.jobportal.specification;

import com.narek.jobportal.entity.Job;
import com.narek.jobportal.entity.JobStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class AdminJobSpecification {
    public static Specification<Job> hasStatus(JobStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Job> minSalary(Double minSalary) {
        return (root, query, cb) -> minSalary == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("salary"), minSalary);
    }

    public static Specification<Job> maxSalary(Double maxSalary) {
        return (root, query, cb) -> maxSalary == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("salary"), maxSalary);
    }

    public static Specification<Job> byEmployer(Long employerId) {
        return (root, query, cb) -> employerId == null ? cb.conjunction() : cb.equal(root.get("employer").get("id"), employerId);
    }

    public static Specification<Job> expiredOnly() {
        return (root, query, cb) -> cb.lessThan(root.get("closingDate"), LocalDate.now());
    }
}
