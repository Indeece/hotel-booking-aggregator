package ru.indeece.bookingservice.kafka;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.indeece.bookingservice.dto.PaymentResultEvent;
import ru.indeece.bookingservice.entities.Booking;
import ru.indeece.bookingservice.enums.BookingStatus;
import ru.indeece.bookingservice.repositories.BookingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentResponseConsumer {

    private final BookingRepository bookingRepository;

    @KafkaListener(topics = {"payment-success-events", "payment-failed-events"}, groupId = "booking-group")
    @Transactional
    public void handlePaymentResponse(ConsumerRecord<String, PaymentResultEvent> record) {
        PaymentResultEvent event = record.value();
        String topic = record.topic();

        Booking booking = bookingRepository.findById(event.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + event.bookingId()));

        if ("payment-success-events".equals(topic)) {
            booking.setStatus(BookingStatus.CONFIRMED);
            log.info("Booking {} CONFIRMED", booking.getId());
        } else {
            booking.setStatus(BookingStatus.REJECTED);
            log.info("Booking {} REJECTED", booking.getId());
        }

        bookingRepository.save(booking);
    }
}
