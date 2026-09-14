package com.starlight.notificationservice.controller;

import com.starlight.notificationservice.model.Notification;
import com.starlight.notificationservice.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/user/{guestName}")
    public List<Notification> getUserNotifications(@PathVariable String guestName) {
        if ("admin".equalsIgnoreCase(guestName) || "all".equalsIgnoreCase(guestName)) {
            return notificationRepository.findAllByOrderByCreatedAtDesc();
        }
        return notificationRepository.findByGuestNameContainingIgnoreCaseOrderByCreatedAtDesc(guestName);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id) {
        return notificationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
