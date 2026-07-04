package ru.indeece.authservice.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.indeece.authservice.entities.User;
import ru.indeece.authservice.repository.UserRepository;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PutMapping("/{id}/balance/deduct")
    @Transactional
    public ResponseEntity<?> deductBalance(@PathVariable Long id, @RequestParam BigDecimal amount) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body("Insufficient funds");
        }

        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);
        return ResponseEntity.ok("Balance deducted successfully");
    }

    @PutMapping("/{id}/balance/deposit")
    @Transactional
    public ResponseEntity<?> depositBalance(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestHeader("X-User-Id") Long authorizedUserId,
            @RequestHeader("X-User-Role") String authorizedUserRole) {

        if (!"MANAGER".equalsIgnoreCase(authorizedUserRole) && !authorizedUserId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only deposit your own balance");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Amount must be greater than zero");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        return ResponseEntity.ok("Balance topped up successfully. New balance: " + user.getBalance());
    }

    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}
