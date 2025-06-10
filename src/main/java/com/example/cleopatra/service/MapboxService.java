package com.example.cleopatra.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
public class MapboxService {

    @Value("${mapbox.access.token}")
    private String accessToken;

    private final String baseUrl = "https://api.mapbox.com";
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        try {
            // Инициализируем RestTemplate
            restTemplate = new RestTemplate();

            // Проверяем что токен загружен
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new IllegalStateException("Mapbox access token не найден");
            }

            // Можно проверить валидность токена (опционально)
            validateToken();

            log.info("MapboxService успешно инициализирован с токеном: {}***",
                    accessToken.substring(0, Math.min(10, accessToken.length())));

        } catch (Exception e) {
            log.error("Ошибка инициализации MapboxService: {}", e.getMessage());
            throw new RuntimeException("Не удалось инициализировать MapboxService", e);
        }
    }

    private void validateToken() {
        try {
            // Простая проверка - пытаемся получить информацию о токене
            String validationUrl = baseUrl + "/tokens/v2?access_token=" + accessToken;
            String response = restTemplate.getForObject(validationUrl, String.class);
            log.info("Токен Mapbox валиден");
        } catch (Exception e) {
            log.warn("Не удалось проверить токен Mapbox: {}", e.getMessage());
            // Не бросаем исключение, так как токен может быть валидным, но API недоступно
        }
    }

    // Генерация статической карты
    public String generateStaticMapUrl(double longitude, double latitude, int zoom, int width, int height) {
        return String.format("%s/styles/v1/mapbox/streets-v11/static/%f,%f,%d/%dx%d?access_token=%s",
                baseUrl, longitude, latitude, zoom, width, height, accessToken);
    }

    // Геокодирование (адрес -> координаты)
    public String getGeocodingUrl(String address) {
        return String.format("%s/geocoding/v5/mapbox.places/%s.json?access_token=%s",
                baseUrl, address, accessToken);
    }

    // Обратное геокодирование (координаты -> адрес)
    public String getReverseGeocodingUrl(double longitude, double latitude) {
        return String.format("%s/geocoding/v5/mapbox.places/%f,%f.json?access_token=%s",
                baseUrl, longitude, latitude, accessToken);
    }

    // Построение маршрута
    public String getDirectionsUrl(String coordinates, String profile) {
        return String.format("%s/directions/v5/mapbox/%s/%s?access_token=%s",
                baseUrl, profile, coordinates, accessToken);
    }

    // Скачивание статической карты
    public byte[] downloadStaticMap(double longitude, double latitude, int zoom, int width, int height) {
        String url = generateStaticMapUrl(longitude, latitude, zoom, width, height);

        try {
            return restTemplate.getForObject(url, byte[].class);
        } catch (Exception e) {
            log.error("Ошибка скачивания карты: {}", e.getMessage());
            throw new RuntimeException("Failed to download map", e);
        }
    }

    // Геттер для проверки инициализации
    public boolean isInitialized() {
        return accessToken != null && !accessToken.trim().isEmpty() && restTemplate != null;
    }
}