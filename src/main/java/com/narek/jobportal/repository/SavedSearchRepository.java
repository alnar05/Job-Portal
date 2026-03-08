package com.narek.jobportal.repository;

import com.narek.jobportal.entity.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {
    List<SavedSearch> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
}
