package com.example.cleopatra.dto.Location;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationSuggestionDto {
    private Long id; // null если место еще не сохранено в БД
    private String placeName;
    private String address;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private Integer distanceMeters; // расстояние от пользователя
    private String mapboxPlaceId;
    private String category; // ресторан, парк, кафе и т.д.
}