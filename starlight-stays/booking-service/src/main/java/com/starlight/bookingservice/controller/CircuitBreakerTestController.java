package com.starlight.bookingservice.controller;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/bookings")
public class CircuitBreakerTestController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/test-cb")
    @CircuitBreaker(name = "roomServiceCB", fallbackMethod = "roomServiceFallback")
    public ResponseEntity<String> testCb() {
        // Attempt synchronous call to the internal room-service network alias
        String response = restTemplate.getForObject("http://room-service:8082/api/rooms", String.class);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<String> roomServiceFallback(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("FALLBACK TRIGGERED: Room Service is unreachable. Circuit is tracking failures.");
    }
}
