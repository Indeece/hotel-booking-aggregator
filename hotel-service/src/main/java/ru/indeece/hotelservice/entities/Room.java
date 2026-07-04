package ru.indeece.hotelservice.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
@Data
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available;

    @ManyToOne
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;
}
