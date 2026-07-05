package ru.indeece.bookingservice.controllers;

import ru.indeece.bookingservice.dto.BookingRequestDto;
import ru.indeece.bookingservice.dto.BookingResponseDto;
import ru.indeece.bookingservice.entities.Booking;
import ru.indeece.bookingservice.services.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking API", description = "Managing bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create booking")
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            @RequestHeader("X-User-Id") Long userId) {

        Booking booking = bookingService.createBooking(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookingResponseDto.fromEntity(booking));
    }
}