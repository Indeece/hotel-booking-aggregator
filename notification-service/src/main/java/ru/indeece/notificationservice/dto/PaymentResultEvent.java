package ru.indeece.notificationservice.dto;

import ru.indeece.notificationservice.enums.PaymentStatus;

public record PaymentResultEvent(
        Long bookingId,
        PaymentStatus status
) {}