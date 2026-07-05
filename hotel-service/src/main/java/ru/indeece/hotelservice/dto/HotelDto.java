package ru.indeece.hotelservice.dto;

import java.util.List;

public record HotelDto(
        Long id,
        String name,
        String description,
        String city,
        String address,
        Integer stars,
        List<RoomDto> rooms
) {}
