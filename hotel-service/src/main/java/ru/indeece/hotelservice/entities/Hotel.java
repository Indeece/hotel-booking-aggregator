package ru.indeece.hotelservice.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "hotels")
@Data
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;
}
