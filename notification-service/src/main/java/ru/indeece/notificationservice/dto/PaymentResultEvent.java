package ru.indeece.notificationservice.dto;

import ru.indeece.notificationservice.PaymentStatus;

public record PaymentResultEvent(
        Long bookingId,
        PaymentStatus status
) {}