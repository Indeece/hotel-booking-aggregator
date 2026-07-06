package ru.indeece.paymentservice.dto;

import java.math.BigDecimal;

public record RoomDto(
        Long id,
        String roomNumber,
        String type,
        BigDecimal pricePerNight,
        Integer capacity
) {}