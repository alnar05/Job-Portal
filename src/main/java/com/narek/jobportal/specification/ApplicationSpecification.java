package com.narek.jobportal.specification;

import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.ApplicationStatus;
import org.springframework.data.jpa.domain.Specification;

public class ApplicationSpecification {
    public static Specification<Application> byCandidate(Long candidateId) {
        return (root, query, cb) -> candidateId == null ? cb.conjunction() : cb.equal(root.get("candidate").get("id"), candidateId);
    }

    public static Specification<Application> byJob(Long jobId) {
        return (root, query, cb) -> jobId == null ? cb.conjunction() : cb.equal(root.get("job").get("id"), jobId);
    }

    public static Specification<Application> byStatus(ApplicationStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }
}
