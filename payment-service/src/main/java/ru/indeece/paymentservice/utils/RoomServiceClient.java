package ru.indeece.paymentservice.utils;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.indeece.paymentservice.dto.RoomDto;

@FeignClient(name = "hotel-service", url = "${services.hotel.url}")
public interface RoomServiceClient {
    @GetMapping("/api/v1/rooms/{id}")
    RoomDto getRoomById(@PathVariable("id") Long id);
}