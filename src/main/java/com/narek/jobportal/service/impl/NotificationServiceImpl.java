package com.narek.jobportal.service.impl;

import com.narek.jobportal.entity.Notification;
import com.narek.jobportal.entity.User;
import com.narek.jobportal.repository.NotificationRepository;
import com.narek.jobportal.service.NotificationService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender javaMailSender;

    public NotificationServiceImpl(NotificationRepository notificationRepository, JavaMailSender javaMailSender) {
        this.notificationRepository = notificationRepository;
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void notify(User user, String subject, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(subject + ": " + message);
        notificationRepository.save(notification);

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(user.getEmail());
            email.setSubject(subject);
            email.setText(message);
            javaMailSender.send(email);
        } catch (Exception ignored) {
        }
    }
}
