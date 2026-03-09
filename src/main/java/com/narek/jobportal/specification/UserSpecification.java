package com.narek.jobportal.specification;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserSpecification {

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> role == null ? cb.conjunction() : cb.isMember(role, root.get("roles"));
    }

    public static Specification<User> hasStatus(Boolean enabled) {
        return (root, query, cb) -> enabled == null ? cb.conjunction() : cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<User> emailContains(String email) {
        return (root, query, cb) -> (email == null || email.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase() + "%");
    }

    public static Specification<User> registeredBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            LocalDateTime start = from != null ? from.atStartOfDay() : LocalDateTime.MIN;
            LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.MAX;
            return cb.between(root.get("createdAt"), start, end);
        };
    }
}
