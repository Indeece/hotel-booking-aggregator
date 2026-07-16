package ru.indeece.bookingservice.services;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.indeece.bookingservice.dto.BookingRequestDto;
import ru.indeece.bookingservice.dto.OrderCreatedEvent;
import ru.indeece.bookingservice.dto.RoomDto;
import ru.indeece.bookingservice.entities.Booking;
import ru.indeece.bookingservice.enums.BookingStatus;
import ru.indeece.bookingservice.repositories.BookingRepository;
import ru.indeece.bookingservice.utils.RoomServiceClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RoomServiceClient roomServiceClient;

    private static final String ORDER_CREATED_TOPIC = "order-created-events";

    public BookingService(
            BookingRepository bookingRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            RoomServiceClient roomServiceClient
    ) {
        this.bookingRepository = bookingRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.roomServiceClient = roomServiceClient;
    }

    @Transactional
    public Booking createBooking(BookingRequestDto request, Long userId) {

        if (request.startDate().isAfter(request.endDate())
                || request.startDate().isEqual(request.endDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        long overlaps = bookingRepository.countOverlappingBookings(
                request.roomId(),
                request.startDate(),
                request.endDate());

        if (overlaps > 0) {
            throw new IllegalStateException("Room is not available for selected dates");
        }

        RoomDto room = roomServiceClient.getRoomById(request.roomId());

        long nights = ChronoUnit.DAYS.between(
                request.startDate(),
                request.endDate());

        BigDecimal totalPrice =
                room.pricePerNight()
                        .multiply(BigDecimal.valueOf(nights));

        Booking booking = Booking.builder()
                .userId(userId)
                .hotelId(request.hotelId())
                .roomId(request.roomId())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedBooking.getId(),
                userId,
                request.roomId(),
                totalPrice
        );

        kafkaTemplate.send(
                ORDER_CREATED_TOPIC,
                String.valueOf(savedBooking.getId()),
                event
        );

        log.info("Booking created [PENDING], total price = {}", totalPrice);

        return savedBooking;
    }
}