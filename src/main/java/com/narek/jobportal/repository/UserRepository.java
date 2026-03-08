package com.narek.jobportal.repository;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("select distinct u from User u join u.roles r where r = :role")
    Page<User> findAllByRole(@Param("role") Role role, Pageable pageable);
}
