package ru.indeece.paymentservice.dto;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long bookingId,
        Long userId,
        Long roomId,
        BigDecimal totalPrice
) {}