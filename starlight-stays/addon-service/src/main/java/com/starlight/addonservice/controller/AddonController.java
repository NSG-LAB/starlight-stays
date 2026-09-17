package com.starlight.addonservice.controller;

import com.starlight.addonservice.model.BookingAddonOrder;
import com.starlight.addonservice.model.ExperienceAddon;
import com.starlight.addonservice.repository.BookingAddonOrderRepository;
import com.starlight.addonservice.repository.ExperienceAddonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/addons")
@SuppressWarnings("null")
public class AddonController {

    private final ExperienceAddonRepository addonRepository;
    private final BookingAddonOrderRepository orderRepository;

    public AddonController(ExperienceAddonRepository addonRepository, BookingAddonOrderRepository orderRepository) {
        this.addonRepository = addonRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public List<ExperienceAddon> getAllAddons() {
        return addonRepository.findByActiveTrueOrderByPriceAsc();
    }

    @GetMapping("/{code}")
    public ResponseEntity<ExperienceAddon> getAddonByCode(@PathVariable String code) {
        return addonRepository.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/attach")
    public ResponseEntity<?> attachAddonToBooking(@RequestBody Map<String, Object> payload) {
        if (!payload.containsKey("bookingId") || !payload.containsKey("addonCode")) {
            return ResponseEntity.badRequest().body("bookingId and addonCode are required");
        }

        Long bookingId = Long.valueOf(payload.get("bookingId").toString());
        String addonCode = payload.get("addonCode").toString();
        Integer quantity = payload.containsKey("quantity") ? Integer.valueOf(payload.get("quantity").toString()) : 1;
        String specialRequests = payload.containsKey("specialRequests") ? payload.get("specialRequests").toString() : "";

        Optional<ExperienceAddon> addonOpt = addonRepository.findByCode(addonCode);
        if (addonOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Experience add-on not found for code: " + addonCode);
        }

        ExperienceAddon addon = addonOpt.get();
        BookingAddonOrder order = new BookingAddonOrder(
                bookingId,
                addon.getCode(),
                addon.getTitle(),
                addon.getPrice(),
                quantity,
                specialRequests
        );

        BookingAddonOrder saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/booking/{bookingId}")
    public Map<String, Object> getBookingAddons(@PathVariable Long bookingId) {
        List<BookingAddonOrder> orders = orderRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        double totalAddonsCost = orders.stream()
                .mapToDouble(o -> (o.getPrice() != null ? o.getPrice() : 0.0) * (o.getQuantity() != null ? o.getQuantity() : 1))
                .sum();

        Map<String, Object> response = new HashMap<>();
        response.put("bookingId", bookingId);
        response.put("addons", orders);
        response.put("addonsCount", orders.size());
        response.put("totalAddonsCost", Math.round(totalAddonsCost * 100.0) / 100.0);
        return response;
    }
}
