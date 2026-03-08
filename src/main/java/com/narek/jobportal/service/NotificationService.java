package com.narek.jobportal.service;

import com.narek.jobportal.entity.User;

public interface NotificationService {
    void notify(User user, String subject, String message);
}
