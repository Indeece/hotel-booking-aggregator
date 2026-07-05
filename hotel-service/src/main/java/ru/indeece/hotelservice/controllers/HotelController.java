package ru.indeece.hotelservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.indeece.hotelservice.dto.HotelCreateRequest;
import ru.indeece.hotelservice.dto.HotelDto;
import ru.indeece.hotelservice.services.HotelService;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private static final String ROLE_MANAGER = "MANAGER";

    @GetMapping
    public ResponseEntity<List<HotelDto>> getHotelsByCity(@RequestParam String city) {
        return ResponseEntity.ok(hotelService.findHotelsByCity(city));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelDto> getHotelById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.findHotelById(id));
    }

    @PostMapping("/create")
    public ResponseEntity<HotelDto> createHotel(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody HotelCreateRequest request) throws AccessDeniedException {

        checkManagerRole(role);
        return new ResponseEntity<>(hotelService.createHotel(request), HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<HotelDto> updateHotel(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id,
            @Valid @RequestBody HotelCreateRequest request) throws AccessDeniedException {

        checkManagerRole(role);
        return ResponseEntity.ok(hotelService.updateHotel(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id) throws AccessDeniedException {

        checkManagerRole(role);
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }

    private void checkManagerRole(String role) throws AccessDeniedException {
        if (!ROLE_MANAGER.equals(role)) {
            throw new AccessDeniedException("Only HOTEL_MANAGER can perform this action");
        }
    }
}