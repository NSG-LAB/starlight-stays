package com.starlight.notificationservice.listener;

import com.starlight.notificationservice.model.Notification;
import com.starlight.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @RabbitListener(queuesToDeclare = @Queue("notification-booking-events"))
    public void handleBookingEvent(String message) {
        log.info("📧 [NOTIFICATION-SERVICE] Received booking event: {}", message);

        Long bookingId = null;
        String guestName = "Valued Guest";
        try {
            Matcher mId = Pattern.compile("\"bookingId\"\\s*:\\s*(\\d+)").matcher(message);
            if (mId.find()) {
                bookingId = Long.parseLong(mId.group(1));
            } else {
                Matcher mRef = Pattern.compile("Booking\\s*#(\\d+)").matcher(message);
                if (mRef.find()) {
                    bookingId = Long.parseLong(mRef.group(1));
                }
            }

            Matcher mGuest = Pattern.compile("\"guestName\"\\s*:\\s*\"([^\"]+)\"").matcher(message);
            if (mGuest.find()) {
                guestName = mGuest.group(1);
            }
        } catch (Exception e) {
            log.warn("Error parsing event string: {}", e.getMessage());
        }

        String voucherCode = "VCHR-STARLIGHT-" + (bookingId != null ? bookingId : UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        String title = "Exclusive Reservation Voucher & Check-in Pass";
        String voucherText = String.format(
            "Greetings %s! Your luxury suite reservation #%s is confirmed. Present voucher code [%s] upon arrival at the Starlight Concierge Lounge.",
            guestName,
            bookingId != null ? bookingId : "N/A",
            voucherCode
        );

        Notification notification = new Notification(bookingId, guestName, voucherCode, title, voucherText);
        notificationRepository.save(notification);

        log.info("✉️ [VOUCHER ISSUED] Created voucher {} for guest '{}' (Booking #{})",
                voucherCode, guestName, bookingId);
    }
}
