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
import ru.indeece.paymentservice.dto.OrderCreatedEvent;
import ru.indeece.paymentservice.dto.PaymentResultEvent;
import ru.indeece.paymentservice.entities.Transaction;
import ru.indeece.paymentservice.enums.PaymentStatus;
import ru.indeece.paymentservice.repositories.TransactionRepository;
import ru.indeece.paymentservice.utils.AuthServiceClient;

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
        log.info("Got payment event: {}", event);
        boolean isSuccess = false;
        BigDecimal amountToDeduct = event.totalPrice();

        try {
            ResponseEntity<String> response = authServiceClient.deductBalance(
                            event.userId(),
                            amountToDeduct
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                isSuccess = true;
                log.info("Payment successful");
            }
        } catch (FeignException.BadRequest e) {
            log.error("Insufficient funds: {}", e.contentUTF8());
        } catch (FeignException.NotFound e) {
            log.error("User not found");
        } catch (Exception e) {
            log.error("Auth Service error: {}", e.getMessage());
        }

        PaymentStatus status = isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILURE;

        transactionRepository.save(
                Transaction.builder()
                        .bookingId(bookingId)
                        .userId(event.userId())
                        .amount(amountToDeduct)
                        .status(status)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        String topic = isSuccess ? "payment-success-events" : "payment-failed-events";

        kafkaTemplate.send(
                topic,
                String.valueOf(bookingId),
                new PaymentResultEvent(bookingId, status)
        );

        log.info("Payment finished. Status={}", status);
    }
}