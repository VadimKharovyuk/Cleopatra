package com.example.cleopatra.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoLocationService {
    private final RestTemplate restTemplate = new RestTemplate();


    /**
     * Получить местоположение по IP (через внешний API)
     */
    public GeoLocationDto getLocationByIp(String ipAddress) {
        try {
            // Пример с ip-api.com (бесплатный сервис)
            String url = "http://ip-api.com/json/" + ipAddress + "?lang=ru";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> data = response.getBody();

            if (data != null && "success".equals(data.get("status"))) {
                return GeoLocationDto.builder()
                        .country((String) data.get("country"))
                        .region((String) data.get("regionName"))
                        .city((String) data.get("city"))
                        .timezone((String) data.get("timezone"))
                        .isp((String) data.get("isp"))
                        .latitude((Double) data.get("lat"))
                        .longitude((Double) data.get("lon"))
                        .build();
            }
        } catch (Exception e) {
            log.warn("Не удалось получить геолокацию для IP {}: {}", ipAddress, e.getMessage());
        }

        return null;
    }
    @Data
    @Builder
    public  static class GeoLocationDto {
        private String country;
        private String region;
        private String city;
        private String timezone;
        private String isp;
        private Double latitude;
        private Double longitude;
    }
}

