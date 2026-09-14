package com.starlight.bookingservice.repository;
import com.starlight.bookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.roomId = :roomId AND b.status = 'CONFIRMED' AND (b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate)")
    boolean existsOverlappingBooking(@Param("roomId") Long roomId, @Param("checkInDate") LocalDate checkInDate, @Param("checkOutDate") LocalDate checkOutDate);

    List<Booking> findByGuestNameContainingIgnoreCase(String guestName);
}
