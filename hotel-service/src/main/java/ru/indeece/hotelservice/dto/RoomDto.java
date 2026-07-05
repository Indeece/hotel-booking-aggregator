package ru.indeece.hotelservice.dto;

import java.math.BigDecimal;

public record RoomDto(
        Long id,
        String roomNumber,
        String type,
        BigDecimal pricePerNight,
        Integer capacity
) {}
