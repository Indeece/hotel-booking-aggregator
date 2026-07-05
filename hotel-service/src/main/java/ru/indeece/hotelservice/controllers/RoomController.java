package ru.indeece.hotelservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.indeece.hotelservice.dto.RoomCreateRequest;
import ru.indeece.hotelservice.dto.RoomDto;
import ru.indeece.hotelservice.services.RoomService;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private static final String ROLE_MANAGER = "MANAGER";

    @GetMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<List<RoomDto>> getRoomsByHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomService.getRoomsByHotelId(hotelId));
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PostMapping("/hotels/{hotelId}/rooms/create")
    public ResponseEntity<RoomDto> createRoom(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long hotelId,
            @Valid @RequestBody RoomCreateRequest request) throws AccessDeniedException {

        checkManagerRole(role);
        return new ResponseEntity<>(roomService.createRoom(hotelId, request), HttpStatus.CREATED);
    }

    @PutMapping("/rooms/update/{id}")
    public ResponseEntity<RoomDto> updateRoom(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id,
            @Valid @RequestBody RoomCreateRequest request) throws AccessDeniedException {

        checkManagerRole(role);
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long id) throws AccessDeniedException {

        checkManagerRole(role);
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    private void checkManagerRole(String role) throws AccessDeniedException {
        if (!ROLE_MANAGER.equals(role)) {
            throw new AccessDeniedException("Only HOTEL_MANAGER can perform this action");
        }
    }
}