package ru.indeece.notificationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.indeece.notificationservice.dto.PaymentResultEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final JavaMailSender mailSender;

    @KafkaListener(topics = {"payment-success-events", "payment-failed-events"}, groupId = "notification-group")
    public void handlePaymentResult(PaymentResultEvent event) {
        log.info("Got an event #{}, status: {}", event.bookingId(), event.status());

        String targetEmail = "user_" + event.bookingId() + "@example.com";
        String subject;
        String text;

        if ("SUCCESS".equals(event.status().name())) {
            subject = "Успешная оплата бронирования #" + event.bookingId();
            text = "Здравствуйте! Ваше бронирование #" + event.bookingId() + " успешно оплачено. Ждем вас в нашем отеле!";
        } else {
            subject = "Ошибка оплаты бронирования #" + event.bookingId();
            text = "Здравствуйте. К сожалению, нам не удалось списать средства за бронь #" + event.bookingId() + ". Пожалуйста, пополните баланс.";
        }

        sendEmail(targetEmail, subject, text);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@hotel-booking.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Mail sent to {}", to);
        } catch (Exception e) {
            log.error("Receive an error while trying to send an email: {}", e.getMessage());
        }
    }
}
