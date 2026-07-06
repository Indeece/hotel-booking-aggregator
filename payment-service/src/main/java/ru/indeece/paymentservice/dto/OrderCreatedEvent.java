package ru.indeece.paymentservice.dto;

public record OrderCreatedEvent(
        Long bookingId,
        Long userId,
        Long roomId
) {}