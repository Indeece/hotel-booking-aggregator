package ru.indeece.paymentservice.utils;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "auth-service", url = "${services.auth.url")
public interface AuthServiceClient {
    @PutMapping("/api/v1/users/{id}/balance/deduct")
    ResponseEntity<String> deductBalance(
            @PathVariable("id") Long id,
            @RequestParam("amount") BigDecimal amount
    );
}
