package com.starlight.bookingservice.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class EventTestController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/test-event")
    public ResponseEntity<String> triggerEvent() {
        String message = "Booking Confirmed - Guest: AMQP Test User " + System.currentTimeMillis();
        // Convert and send directly to the default exchange, routing to our queue
        rabbitTemplate.convertAndSend("booking-events", message);
        rabbitTemplate.convertAndSend("notification-booking-events", message);
        return ResponseEntity.ok("Event dispatched to RabbitMQ: " + message);
    }
}
