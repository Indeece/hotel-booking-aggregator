package ru.indeece.bookingservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.indeece.bookingservice.entities.Booking;

import java.time.LocalDate;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.roomId = :roomId " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "AND (b.startDate < :endDate AND b.endDate > :startDate)")
    long countOverlappingBookings(@Param("roomId") Long roomId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}