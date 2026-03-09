package com.narek.jobportal.repository;

import com.narek.jobportal.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    List<Application> findByCandidateId(Long candidateId);
    List<Application> findByJobId(Long jobId);
    boolean existsByJobIdAndCandidateId(Long id, Long id1);
    void deleteByJobId(Long jobId);
    void deleteByCandidateId(Long candidateId);
    List<Application> findTop5ByOrderByAppliedAtDesc();
}
