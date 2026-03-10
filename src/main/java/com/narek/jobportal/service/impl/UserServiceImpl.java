package com.narek.jobportal.service.impl;

import com.narek.jobportal.dto.AdminUserFilterDto;
import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.service.UserService;
import com.narek.jobportal.specification.UserSpecification;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final SessionRegistry sessionRegistry;

    public UserServiceImpl(UserRepository userRepository,
                           ApplicationRepository applicationRepository,
                           SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));

        if (user.getCandidate() != null && user.getCandidate().getId() != null) {
            applicationRepository.deleteByCandidateId(user.getCandidate().getId());
        }

        userRepository.delete(user);
    }

    @Override
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findAllByRole(role, pageable);
    }

    @Override
    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));
        user.setEnabled(enabled);
        userRepository.save(user);
        if (!enabled) {
            expireUserSessions(user.getEmail());
        }
    }

    @Override
    public Page<User> searchUsers(AdminUserFilterDto filter, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecification.hasRole(filter.getRole()))
                .and(UserSpecification.hasStatus(filter.getEnabled()))
                .and(UserSpecification.emailContains(filter.getEmail()))
                .and(UserSpecification.registeredBetween(filter.getRegisteredFrom(), filter.getRegisteredTo()));
        return userRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public void setEnabledBulk(List<Long> userIds, boolean enabled) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<User> users = userRepository.findAllById(userIds);
        users.forEach(user -> user.setEnabled(enabled));
        userRepository.saveAll(users);
        if (!enabled) {
            users.stream()
                    .map(User::getEmail)
                    .forEach(this::expireUserSessions);
        }
    }

    private void expireUserSessions(String email) {
        sessionRegistry.getAllPrincipals().forEach(principal -> {
            if (principal instanceof UserDetails userDetails
                    && userDetails.getUsername().equals(email)) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                sessions.forEach(SessionInformation::expireNow);
            }
        });
    }

    @Override
    public List<User> getRecentUsers(int limit) {
        return userRepository.findTop5ByOrderByCreatedAtDesc().stream().limit(limit).toList();
    }
}
