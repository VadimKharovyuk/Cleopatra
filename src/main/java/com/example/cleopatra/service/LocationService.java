package com.example.cleopatra.service;

import com.example.cleopatra.model.Location;
import com.example.cleopatra.repository.LocationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;
    private final MapboxService mapboxService;

    public Location findById(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Локация с ID " + locationId + " не найдена"));
    }

    public Location createLocationFromCoordinates(Double latitude, Double longitude, String placeName) {
        // Проверяем, есть ли уже похожая локация (с погрешностью 100м)
        Optional<Location> existingLocation = locationRepository
                .findByCoordinatesWithPrecision(latitude, longitude, 0.001);

        if (existingLocation.isPresent()) {
            log.info("Найдена существующая локация рядом с координатами {}, {}", latitude, longitude);
            return existingLocation.get();
        }

        // Создаем новую локацию
        Location location = Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .placeName(placeName)
                .build();

        // Опционально: получаем адрес через обратное геокодирование
        try {
            enrichLocationWithAddress(location);
        } catch (Exception e) {
            log.warn("Не удалось получить адрес для координат {}, {}: {}",
                    latitude, longitude, e.getMessage());
        }

        return locationRepository.save(location);
    }

    private void enrichLocationWithAddress(Location location) {
        try {
            log.debug("Обогащение локации адресной информацией для координат {}, {}",
                    location.getLatitude(), location.getLongitude());

            // Получаем URL для обратного геокодирования
            String reverseGeocodingUrl = mapboxService.getReverseGeocodingUrl(
                    location.getLongitude(), location.getLatitude());

            // ✅ ИСПРАВЛЕНО: используем RestTemplate из MapboxService
            RestTemplate restTemplate = mapboxService.getRestTemplate();
            String response = restTemplate.getForObject(reverseGeocodingUrl, String.class);

            if (response != null) {
                // Парсим ответ и обновляем локацию
                parseAndUpdateLocation(location, response);
                log.info("Локация успешно обогащена адресной информацией: {}", location.getAddress());
            }

        } catch (Exception e) {
            log.warn("Не удалось обогатить локацию адресной информацией для координат {}, {}: {}",
                    location.getLatitude(), location.getLongitude(), e.getMessage());
            // Не бросаем исключение - локация может существовать без адреса
        }
    }

    private void parseAndUpdateLocation(Location location, String mapboxResponse) {
        try {
            // Используем Jackson для парсинга JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(mapboxResponse);

            JsonNode features = rootNode.get("features");
            if (features != null && features.isArray() && features.size() > 0) {

                // Берем первый результат (самый релевантный)
                JsonNode firstFeature = features.get(0);

                // Извлекаем основную информацию
                String placeName = getTextValue(firstFeature, "place_name");
                String mapboxPlaceId = getTextValue(firstFeature, "id");

                // Обновляем основные поля
                if (placeName != null) {
                    location.setAddress(placeName);
                }
                if (mapboxPlaceId != null) {
                    location.setMapboxPlaceId(mapboxPlaceId);
                }

                // Извлекаем детальную информацию из context
                JsonNode context = firstFeature.get("context");
                if (context != null && context.isArray()) {
                    for (JsonNode contextItem : context) {
                        String id = getTextValue(contextItem, "id");
                        String text = getTextValue(contextItem, "text");

                        if (id != null && text != null) {
                            // Определяем тип по префиксу ID
                            if (id.startsWith("place.")) {
                                location.setCity(text);
                            } else if (id.startsWith("country.")) {
                                location.setCountry(text);
                            }
                        }
                    }
                }

                // Если название места не было задано пользователем, используем из Mapbox
                if (location.getPlaceName() == null || location.getPlaceName().trim().isEmpty()) {
                    JsonNode properties = firstFeature.get("properties");
                    if (properties != null) {
                        String category = getTextValue(properties, "category");
                        if (category != null) {
                            location.setPlaceName(category);
                        }
                    }
                }

                log.debug("Парсинг Mapbox ответа завершен. Адрес: {}, Город: {}, Страна: {}",
                        location.getAddress(), location.getCity(), location.getCountry());

            } else {
                log.warn("Mapbox не вернул результатов для координат {}, {}",
                        location.getLatitude(), location.getLongitude());
            }

        } catch (Exception e) {
            log.error("Ошибка при парсинге ответа Mapbox: {}", e.getMessage());
        }
    }

    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    // Дополнительный метод для получения детальной информации о месте
    public Location enrichLocationWithPlaceDetails(Location location) {
        if (location.getMapboxPlaceId() != null) {
            try {
                // Запрос детальной информации о месте по Place ID
                String detailsUrl = String.format("%s/geocoding/v5/mapbox.places/%s.json?access_token=%s",
                        mapboxService.getBaseUrl(),
                        location.getMapboxPlaceId(),
                        mapboxService.getAccessToken());

                // ✅ ИСПРАВЛЕНО: используем RestTemplate из MapboxService
                RestTemplate restTemplate = mapboxService.getRestTemplate();
                String response = restTemplate.getForObject(detailsUrl, String.class);
                if (response != null) {
                    parseAndUpdateLocation(location, response);
                }

            } catch (Exception e) {
                log.warn("Не удалось получить детальную информацию о месте {}: {}",
                        location.getMapboxPlaceId(), e.getMessage());
            }
        }
        return location;
    }
}