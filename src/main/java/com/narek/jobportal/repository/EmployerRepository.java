package com.narek.jobportal.repository;

import com.narek.jobportal.entity.Employer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerRepository extends JpaRepository<Employer, Long> {
    Optional<Employer> findByUserId(Long userId);
    Optional<Employer> findByUserEmail(String email);
}
