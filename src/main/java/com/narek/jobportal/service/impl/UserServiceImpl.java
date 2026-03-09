package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.Role;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.entity.UserStatus;
import com.narek.jobportal.repository.ApplicationRepository;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public UserServiceImpl(UserRepository userRepository, ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
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
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findAllByRole(role, pageable);
    }

    @Override
    public void setEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));
        if (user.getStatus() == UserStatus.BANNED && enabled) {
            throw new IllegalArgumentException("BANNED users cannot be re-enabled from toggle action");
        }
        user.setEnabled(enabled);
        if (user.getStatus() != UserStatus.BANNED) {
            user.setStatus(enabled ? UserStatus.ACTIVE : UserStatus.DISABLED);
        }
        userRepository.save(user);
    }

    @Override
    public void updateStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));
        if (user.getStatus() == UserStatus.BANNED && status != UserStatus.BANNED) {
            throw new IllegalArgumentException("BANNED users require manual admin change and cannot be reverted here");
        }
        user.setStatus(status);
        user.setEnabled(status == UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public void updateRoles(Long id, Role role, boolean add) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));
        HashSet<Role> roles = new HashSet<>(user.getRoles());
        if (add) {
            roles.add(role);
        } else {
            roles.remove(role);
        }
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public void resetPassword(Long id, String encodedPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }
}
