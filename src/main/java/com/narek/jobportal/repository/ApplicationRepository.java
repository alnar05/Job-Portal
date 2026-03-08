package com.narek.jobportal.repository;

import com.narek.jobportal.entity.Application;
import com.narek.jobportal.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByCandidateId(Long candidateId);
    List<Application> findByJobId(Long jobId);
    boolean existsByJobIdAndCandidateId(Long id, Long id1);
    void deleteByJobId(Long jobId);
    void deleteByCandidateId(Long candidateId);

    Long countByJobId(Long jobId);

    @Query("select count(a) from Application a where a.job.employer.id = :employerId")
    Long countByEmployerId(Long employerId);

    List<Application> findByJobIdAndStatusIn(Long jobId, List<ApplicationStatus> statuses);
}
