package ru.indeece.authservice.entities;

import jakarta.persistence.*;
import lombok.Data;
import ru.indeece.authservice.enums.Role;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
}
