package ru.indeece.bookingservice.dto;

public record OrderCreatedEvent(
        Long bookingId,
        Long userId,
        Long roomId
) {}