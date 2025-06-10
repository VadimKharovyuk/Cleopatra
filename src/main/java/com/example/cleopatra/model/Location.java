package com.example.cleopatra.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "locations")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ ИСПРАВЛЕНО: убираем precision и scale для DOUBLE
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "mapbox_place_id")
    private String mapboxPlaceId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

//@Entity
//@Table(name = "locations")
//@AllArgsConstructor
//@NoArgsConstructor
//@Getter
//@Setter
//@Builder
//public class Location {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, precision = 10, scale = 8)
//    private Double latitude;
//
//    @Column(nullable = false, precision = 11, scale = 8)
//    private Double longitude;
//
//    @Column(name = "address")
//    private String address; // Читаемый адрес
//
//    @Column(name = "city")
//    private String city;
//
//    @Column(name = "country")
//    private String country;
//
//    @Column(name = "place_name")
//    private String placeName; // Название места (кафе, парк и т.д.)
//
//    // Для Mapbox
//    @Column(name = "mapbox_place_id")
//    private String mapboxPlaceId;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//    }
