package ru.indeece.bookingservice.dto;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long bookingId,
        Long userId,
        Long roomId,
        BigDecimal totalPrice
) {}