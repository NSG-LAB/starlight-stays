package com.starlight.bookingservice.controller;

import com.starlight.bookingservice.client.RoomClient;
import com.starlight.bookingservice.dto.RoomDto;
import com.starlight.bookingservice.model.Booking;
import com.starlight.bookingservice.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingRepository bookingRepository;
    private final RoomClient roomClient;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    public BookingController(BookingRepository bookingRepository, RoomClient roomClient) {
        this.bookingRepository = bookingRepository;
        this.roomClient = roomClient;
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<?> createBooking(@RequestBody Booking booking) {
        log.info("Incoming booking request for room #{} by guest '{}'", booking.getRoomId(), booking.getGuestName());

        // 1. Verify Room existence & availability via Room Service Feign client
        RoomDto room = null;
        try {
            room = roomClient.getRoomById(booking.getRoomId());
        } catch (Exception e) {
            log.warn("Room service lookup error for room #{}: {}", booking.getRoomId(), e.getMessage());
        }

        if (room == null || !room.isAvailable()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room does not exist or is currently unavailable.");
        }

        // 2. Prevent Double Booking
        if (bookingRepository.existsOverlappingBooking(booking.getRoomId(), booking.getCheckInDate(), booking.getCheckOutDate())) {
            log.warn("Double booking conflict detected for room #{} between {} and {}", 
                    booking.getRoomId(), booking.getCheckInDate(), booking.getCheckOutDate());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Room is unavailable for selected dates.");
        }

        // 3. Persist Booking
        if (booking.getGuestCount() == null || booking.getGuestCount() < 1) {
            booking.setGuestCount(1);
        }
        booking.setStatus("CONFIRMED");
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking #{} successfully confirmed and saved.", savedBooking.getId());

        // 4. Trigger Transactional Saga: Publish AMQP Event to RabbitMQ
        if (rabbitTemplate != null) {
            try {
                boolean simulateFailure = (savedBooking.getGuestName() != null && 
                    (savedBooking.getGuestName().toUpperCase().contains("DECLINE") || 
                     savedBooking.getGuestName().toUpperCase().contains("FAIL")));

                String eventMessage = String.format(
                    "{\"bookingId\":%d,\"roomId\":%d,\"guestName\":\"%s\",\"guestCount\":%d,\"simulatePaymentFailure\":%b}",
                    savedBooking.getId(),
                    savedBooking.getRoomId(),
                    savedBooking.getGuestName() != null ? savedBooking.getGuestName().replace("\"", "\\\"") : "Guest",
                    savedBooking.getGuestCount(),
                    simulateFailure
                );
                rabbitTemplate.convertAndSend("booking-events", eventMessage);
                log.info("Dispatched booking event to RabbitMQ [booking-events]: {}", eventMessage);
            } catch (Exception e) {
                log.error("RabbitMQ event dispatch failed: {}. Continuing with confirmed booking.", e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedBooking);
    }

    @GetMapping("/user/{guestName}")
    public ResponseEntity<List<Booking>> getBookingsByGuest(@PathVariable String guestName) {
        if ("all".equalsIgnoreCase(guestName) || "admin".equalsIgnoreCase(guestName)) {
            return ResponseEntity.ok(bookingRepository.findAll());
        }
        return ResponseEntity.ok(bookingRepository.findByGuestNameContainingIgnoreCase(guestName));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long id) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            log.info("Booking #{} marked as CANCELLED.", id);
            return ResponseEntity.ok("Booking #" + id + " cancelled successfully.");
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Booking not found"));
    }
}
