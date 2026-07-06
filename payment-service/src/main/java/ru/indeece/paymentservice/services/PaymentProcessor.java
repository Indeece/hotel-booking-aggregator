package ru.indeece.paymentservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.indeece.paymentservice.enums.PaymentStatus;
import ru.indeece.paymentservice.utils.AuthServiceClient;
import ru.indeece.paymentservice.dto.OrderCreatedEvent;
import ru.indeece.paymentservice.dto.PaymentResultEvent;
import ru.indeece.paymentservice.entities.Transaction;
import ru.indeece.paymentservice.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionRepository transactionRepository;
    private final AuthServiceClient authServiceClient;

    @KafkaListener(topics = "order-created-events", groupId = "payment-group")
    @Transactional
    public void processPayment(ConsumerRecord<String, OrderCreatedEvent> record) {
        OrderCreatedEvent event = record.value();
        Long bookingId = event.id();
        log.info("Got a payment event: {}", event);

        boolean isSuccess = false;
        try {
            isSuccess = authServiceClient.deductBalance(event.userId(), 5000.0);
            log.info("Response from client: {}", isSuccess);
        } catch (Exception e) {
            log.error("Issue: {}", e.getMessage());
            isSuccess = false;
        }

        PaymentStatus status = isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILURE;

        transactionRepository.save(Transaction.builder()
                .bookingId(bookingId)
                .userId(event.userId())
                .amount(BigDecimal.valueOf(5000.0))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build());

        String targetTopic = isSuccess ? "payment-success-events" : "payment-failed-events";
        kafkaTemplate.send(targetTopic, String.valueOf(bookingId), new PaymentResultEvent(bookingId, status));

        log.info("Handled payment event: {}", event);
    }
}