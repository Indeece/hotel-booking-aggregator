package ru.indeece.hotelservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record HotelCreateRequest(
        @NotBlank
        String name,

        String description,

        @NotBlank
        String city,

        @NotBlank
        String address,

        @Min(value = 1, message = "At least 1 star")
        @Max(value = 5, message = "At most 5 stars")
        Integer stars
) {}
