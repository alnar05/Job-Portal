package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.AdminAuditLog;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.AdminAuditLogRepository;
import com.narek.jobportal.repository.UserRepository;
import com.narek.jobportal.service.AdminAuditService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditServiceImpl implements AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final UserRepository userRepository;

    public AdminAuditServiceImpl(AdminAuditLogRepository adminAuditLogRepository, UserRepository userRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void log(String action, String details) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(email).orElse(null);
        if (admin == null) {
            return;
        }
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(admin);
        log.setAction(action);
        log.setDetails(details);
        adminAuditLogRepository.save(log);
    }
}
