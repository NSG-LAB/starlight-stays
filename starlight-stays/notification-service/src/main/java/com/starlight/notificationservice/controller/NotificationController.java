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
    private final com.starlight.notificationservice.service.PdfGeneratorService pdfGeneratorService;

    public NotificationController(NotificationRepository notificationRepository,
                                  com.starlight.notificationservice.service.PdfGeneratorService pdfGeneratorService) {
        this.notificationRepository = notificationRepository;
        this.pdfGeneratorService = pdfGeneratorService;
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

    @GetMapping("/pdf/{bookingId}")
    public ResponseEntity<byte[]> getVoucherPdf(@PathVariable Long bookingId) {
        Notification notification = notificationRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Notification mock = new Notification();
                    mock.setBookingId(bookingId);
                    mock.setGuestName("VIP Resident");
                    mock.setRoomName("Celestial Observatory Penthouse");
                    mock.setTotalPrice(1250.0);
                    mock.setVoucherCode("STR-VOUCHER-" + bookingId);
                    mock.setSecurityHash("0x" + Long.toHexString(bookingId * 314159265L).toUpperCase());
                    mock.setCreatedAt(java.time.LocalDateTime.now());
                    return mock;
                });

        byte[] pdfBytes = pdfGeneratorService.generateLuxuryVoucherPdf(notification);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Starlight-Voucher-" + bookingId + ".pdf\"")
                .body(pdfBytes);
    }
}
