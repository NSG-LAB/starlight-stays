package com.starlight.notificationservice.repository;

import com.starlight.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByGuestNameContainingIgnoreCaseOrderByCreatedAtDesc(String guestName);
    List<Notification> findAllByOrderByCreatedAtDesc();
}
