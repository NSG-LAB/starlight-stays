package com.starlight.bookingservice.listener;

import com.starlight.bookingservice.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PaymentFailureListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailureListener.class);

    @Autowired
    private BookingRepository bookingRepository;

    @RabbitListener(queuesToDeclare = @Queue("payment-failed-events"))
    public void handlePaymentFailure(String message) {
        log.warn("🚨 [COMPENSATING TRANSACTION] Received payment failure event: {}", message);

        Long bookingId = null;
        try {
            Matcher m = Pattern.compile("\"bookingId\"\\s*:\\s*(\\d+)").matcher(message);
            if (m.find()) {
                bookingId = Long.parseLong(m.group(1));
            }
        } catch (Exception e) {
            log.error("Failed to parse bookingId from compensating event: {}", e.getMessage());
        }

        if (bookingId != null) {
            final Long targetId = bookingId;
            bookingRepository.findById(targetId).ifPresentOrElse(booking -> {
                booking.setStatus("CANCELLED_PAYMENT_FAILED");
                bookingRepository.save(booking);
                log.info("🛡️ [SAGA ROLLBACK COMPLETE] Booking #{} rolled back to CANCELLED_PAYMENT_FAILED. Room #{} dates released.",
                        targetId, booking.getRoomId());
            }, () -> {
                log.warn("Booking #{} not found during saga rollback compensation.", targetId);
            });
        }
    }
}
