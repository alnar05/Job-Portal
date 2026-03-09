package com.narek.jobportal.service;

import com.narek.jobportal.dto.AdminUserFilterDto;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    Optional<User> getUserById(Long id);
    Optional<User> getUserByEmail(String email);
    void deleteUser(Long id);
    Page<User> getUsersByRole(Role role, Pageable pageable);
    void setEnabled(Long id, boolean enabled);
    Page<User> searchUsers(AdminUserFilterDto filter, Pageable pageable);
    void setEnabledBulk(List<Long> userIds, boolean enabled);
    List<User> getRecentUsers(int limit);
}
