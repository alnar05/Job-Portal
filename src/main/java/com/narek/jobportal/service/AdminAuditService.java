package com.narek.jobportal.service;

public interface AdminAuditService {
    void log(String action, String details);
}
