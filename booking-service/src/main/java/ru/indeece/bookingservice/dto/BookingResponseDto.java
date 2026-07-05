package ru.indeece.bookingservice.dto;

import ru.indeece.bookingservice.entities.Booking;

import java.time.LocalDate;

public record BookingResponseDto(
        Long id,
        Long hotelId,
        Long roomId,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
    public static BookingResponseDto fromEntity(Booking booking) {
        return new BookingResponseDto(
                booking.getId(),
                booking.getHotelId(),
                booking.getRoomId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus().name()
        );
    }
}
