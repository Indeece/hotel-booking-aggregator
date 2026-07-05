package ru.indeece.hotelservice.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record RoomCreateRequest(
        @NotBlank
        String roomNumber,

        @NotBlank
        String type,

        BigDecimal pricePerNight,

        Integer capacity
) {
}