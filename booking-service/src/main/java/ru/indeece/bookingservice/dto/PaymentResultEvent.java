package ru.indeece.bookingservice.dto;

import ru.indeece.bookingservice.enums.PaymentStatus;

public record PaymentResultEvent(
        Long bookingId,
        PaymentStatus status
) {}