package com.narek.jobportal.repository;

import com.narek.jobportal.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByCandidateId(Long candidateId);
    List<Application> findByJobId(Long jobId);
    boolean existsByJobIdAndCandidateId(Long id, Long id1);
    void deleteByJobId(Long jobId);
}
