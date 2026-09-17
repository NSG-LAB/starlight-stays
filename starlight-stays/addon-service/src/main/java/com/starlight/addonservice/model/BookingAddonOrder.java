package com.starlight.addonservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_addon_orders")
public class BookingAddonOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private String addonCode;

    private String addonTitle;
    private Double price;
    private Integer quantity = 1;

    @Column(columnDefinition = "TEXT")
    private String specialRequests;

    private LocalDateTime createdAt;

    public BookingAddonOrder() {
        this.createdAt = LocalDateTime.now();
    }

    public BookingAddonOrder(Long bookingId, String addonCode, String addonTitle, Double price, Integer quantity, String specialRequests) {
        this.bookingId = bookingId;
        this.addonCode = addonCode;
        this.addonTitle = addonTitle;
        this.price = price;
        this.quantity = quantity != null ? quantity : 1;
        this.specialRequests = specialRequests;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getAddonCode() { return addonCode; }
    public void setAddonCode(String addonCode) { this.addonCode = addonCode; }

    public String getAddonTitle() { return addonTitle; }
    public void setAddonTitle(String addonTitle) { this.addonTitle = addonTitle; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
