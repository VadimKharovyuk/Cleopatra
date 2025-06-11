package com.example.cleopatra.dto.Location;

import com.example.cleopatra.model.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto {
    private Long id;
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private String country;
    private String placeName;
    private String mapboxPlaceId;
    private LocalDateTime createdAt;

    // ✅ ДОБАВЛЯЕМ УДОБНЫЕ МЕТОДЫ ДЛЯ ОТОБРАЖЕНИЯ
    public String getDisplayName() {
        if (placeName != null && !placeName.trim().isEmpty() && !placeName.startsWith("Геолокация")) {
            return placeName;
        }
        if (city != null && country != null) {
            return city + ", " + country;
        }
        if (city != null) {
            return city;
        }
        if (country != null) {
            return country;
        }
        return "Неизвестное место";
    }

    public String getShortAddress() {
        if (city != null && country != null) {
            return city + ", " + country;
        }
        return getDisplayName();
    }

    // ✅ ДОБАВЛЯЕМ ПРОВЕРКУ НАЛИЧИЯ ГЕОЛОКАЦИИ
    public boolean hasValidLocation() {
        return latitude != null && longitude != null;
    }

    // ✅ ДОБАВЛЯЕМ МЕТОД ДЛЯ ПОЛУЧЕНИЯ КООРДИНАТ В СТРОКОВОМ ВИДЕ
    public String getCoordinatesString() {
        if (hasValidLocation()) {
            return String.format("%.4f, %.4f", latitude, longitude);
        }
        return "";
    }

    // ✅ ДОБАВЛЯЕМ ПРОВЕРКУ НАЛИЧИЯ АДРЕСНОЙ ИНФОРМАЦИИ
    public boolean hasAddressInfo() {
        return (address != null && !address.trim().isEmpty()) ||
                (city != null && !city.trim().isEmpty()) ||
                (country != null && !country.trim().isEmpty());
    }

    // Статический метод для конвертации из Entity
    public static LocationDto from(Location location) {
        if (location == null) {
            return null;
        }

        return LocationDto.builder()
                .id(location.getId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .address(location.getAddress())
                .city(location.getCity())
                .country(location.getCountry())
                .placeName(location.getPlaceName())
                .mapboxPlaceId(location.getMapboxPlaceId())
                .createdAt(location.getCreatedAt())
                .build();
    }
}