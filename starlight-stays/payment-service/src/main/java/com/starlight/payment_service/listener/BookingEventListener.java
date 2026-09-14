package com.starlight.payment_service.listener;

import com.starlight.payment_service.entity.Payment;
import com.starlight.payment_service.repository.PaymentRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BookingEventListener {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queuesToDeclare = @org.springframework.amqp.rabbit.annotation.Queue("booking-events"))
    public void handleBookingEvent(String message) {
        System.out.println("==================================================");
        System.out.println("💳 [PAYMENT-SERVICE] Received Booking Event: " + message);

        Long bookingId = null;
        try {
            Matcher m1 = Pattern.compile("\"bookingId\"\\s*:\\s*(\\d+)").matcher(message);
            if (m1.find()) {
                bookingId = Long.parseLong(m1.group(1));
            } else {
                Matcher m2 = Pattern.compile("Booking\\s*#(\\d+)").matcher(message);
                if (m2.find()) {
                    bookingId = Long.parseLong(m2.group(1));
                }
            }
        } catch (Exception ignored) {}

        boolean shouldFail = message.contains("DECLINE") || 
                             message.contains("\"simulatePaymentFailure\":true") || 
                             message.contains("\"simulatePaymentFailure\": true") ||
                             message.contains("FAIL_PAYMENT");

        Payment payment = new Payment();
        payment.setBookingReference(message);
        payment.setAmount(150.00);
        payment.setProcessedAt(LocalDateTime.now());

        if (shouldFail) {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);

            String safeRef = message.replace("\"", "'");
            String failPayload = "{\"bookingId\": " + (bookingId != null ? bookingId : 0) 
                    + ", \"bookingReference\": \"" + safeRef + "\", \"status\": \"FAILED\", \"reason\": \"Card declined / Insufficient funds\", \"timestamp\": \"" + LocalDateTime.now() + "\"}";
            messagingTemplate.convertAndSend("/topic/payments", failPayload);

            // Publish Compensating Transaction Event to RabbitMQ
            if (bookingId != null) {
                String compensatingPayload = "{\"bookingId\": " + bookingId + ", \"reason\": \"PAYMENT_DECLINED\"}";
                rabbitTemplate.convertAndSend("payment-failed-events", compensatingPayload);
                System.out.println("⚠️ [PAYMENT-SERVICE] Compensating Event Dispatched to [payment-failed-events]: " + compensatingPayload);
            }
        } else {
            payment.setStatus("COMPLETED");
            paymentRepository.save(payment);

            String safeRef = message.replace("\"", "'");
            String successPayload = "{\"bookingId\": " + (bookingId != null ? bookingId : 0) 
                    + ", \"bookingReference\": \"" + safeRef + "\", \"status\": \"COMPLETED\", \"timestamp\": \"" + LocalDateTime.now() + "\"}";
            messagingTemplate.convertAndSend("/topic/payments", successPayload);

            System.out.println("💳 [PAYMENT-SERVICE] Payment Completed & Broadcasted.");
        }
        System.out.println("==================================================");
    }
}
