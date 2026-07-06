package ru.indeece.paymentservice.dto;

import ru.indeece.paymentservice.enums.PaymentStatus;

public record PaymentResultEvent(
        Long bookingId,
        PaymentStatus status
) {}