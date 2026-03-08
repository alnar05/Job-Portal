package com.narek.jobportal.service;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    Optional<User> getUserById(Long id);
    Optional<User> getUserByEmail(String email);
    void deleteUser(Long id);
    Page<User> getUsersByRole(Role role, Pageable pageable);
    void setEnabled(Long id, boolean enabled);
    void updateStatus(Long id, UserStatus status);
    void updateRoles(Long id, Role role, boolean add);
    void resetPassword(Long id, String encodedPassword);
}
