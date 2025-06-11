package com.example.cleopatra.model;
import com.example.cleopatra.enums.DeviceType;
import com.example.cleopatra.enums.StatType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ad_statistics")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с рекламой
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id", nullable = false)
    private Advertisement advertisement;

    // Кто взаимодействовал (может быть null для анонимных пользователей)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Когда произошло
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Дополнительная информация для аналитики
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "referer")
    private String referer;

    // Геолокация (если доступна)
    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatType type;

    // Устройство пользователя
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType;

    // Стоимость этого действия
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
