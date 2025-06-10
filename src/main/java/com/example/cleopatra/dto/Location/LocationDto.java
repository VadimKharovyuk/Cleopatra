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