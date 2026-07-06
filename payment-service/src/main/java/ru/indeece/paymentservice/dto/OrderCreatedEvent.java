package ru.indeece.paymentservice.dto;

public record OrderCreatedEvent(
        Long id,
        Long userId,
        Long roomId
) {}