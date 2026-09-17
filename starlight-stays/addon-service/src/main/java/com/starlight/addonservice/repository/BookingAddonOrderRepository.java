package com.starlight.addonservice.repository;

import com.starlight.addonservice.model.BookingAddonOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@SuppressWarnings("null")
public interface BookingAddonOrderRepository extends JpaRepository<BookingAddonOrder, Long> {
    List<BookingAddonOrder> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
}
