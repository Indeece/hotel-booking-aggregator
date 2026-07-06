package ru.indeece.paymentservice.services;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.http.ResponseEntity;
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
        Long bookingId = event.bookingId();
        log.info("Got a payment event: {}", event);
        BigDecimal amountToDeduct = BigDecimal.valueOf(5000.00); // TODO заглушка, потом поменять

        boolean isSuccess = false;
        try {
            ResponseEntity<String> response = authServiceClient.deductBalance(event.userId(), amountToDeduct);

            if (response.getStatusCode().is2xxSuccessful()) {
                isSuccess = true;
                log.info("Payment successful! Auth Service response: {}", response.getBody());
            }

        } catch (FeignException.BadRequest e) {
            log.error("Payment refused for user #{}. Reason: {}", event.userId(), e.contentUTF8());
            isSuccess = false;
        } catch (FeignException.NotFound e) {
            log.error("User #{} not found in the system", event.userId());
            isSuccess = false;
        } catch (Exception e) {
            log.error("Critical connection error with Auth Service: {}", e.getMessage());
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