//package com.example.cleopatra.service;
//
//import com.example.cleopatra.model.Location;
//import com.example.cleopatra.repository.LocationRepository;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Optional;
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class LocationService {
//
//    private final LocationRepository locationRepository;
//    private final MapboxService mapboxService;
//
//    public Location findById(Long locationId) {
//        return locationRepository.findById(locationId)
//                .orElseThrow(() -> new RuntimeException("Локация с ID " + locationId + " не найдена"));
//    }
//
//    public Location createLocationFromCoordinates(Double latitude, Double longitude, String placeName) {
//        // Проверяем, есть ли уже похожая локация (с погрешностью 100м)
//        Optional<Location> existingLocation = locationRepository
//                .findByCoordinatesWithPrecision(latitude, longitude, 0.001);
//
//        if (existingLocation.isPresent()) {
//            log.info("Найдена существующая локация рядом с координатами {}, {}", latitude, longitude);
//            return existingLocation.get();
//        }
//
//        // Создаем новую локацию
//        Location location = Location.builder()
//                .latitude(latitude)
//                .longitude(longitude)
//                .placeName(placeName)
//                .build();
//
//        // Опционально: получаем адрес через обратное геокодирование
//        try {
//            enrichLocationWithAddress(location);
//        } catch (Exception e) {
//            log.warn("Не удалось получить адрес для координат {}, {}: {}",
//                    latitude, longitude, e.getMessage());
//        }
//
//        return locationRepository.save(location);
//    }
//
//    private void enrichLocationWithAddress(Location location) {
//        try {
//            log.debug("Обогащение локации адресной информацией для координат {}, {}",
//                    location.getLatitude(), location.getLongitude());
//
//            // Получаем URL для обратного геокодирования
//            String reverseGeocodingUrl = mapboxService.getReverseGeocodingUrl(
//                    location.getLongitude(), location.getLatitude());
//
//            // ✅ ИСПРАВЛЕНО: используем RestTemplate из MapboxService
//            RestTemplate restTemplate = mapboxService.getRestTemplate();
//            String response = restTemplate.getForObject(reverseGeocodingUrl, String.class);
//
//            if (response != null) {
//                // Парсим ответ и обновляем локацию
//                parseAndUpdateLocation(location, response);
//                log.info("Локация успешно обогащена адресной информацией: {}", location.getAddress());
//            }
//
//        } catch (Exception e) {
//            log.warn("Не удалось обогатить локацию адресной информацией для координат {}, {}: {}",
//                    location.getLatitude(), location.getLongitude(), e.getMessage());
//            // Не бросаем исключение - локация может существовать без адреса
//        }
//    }
//
//    private void parseAndUpdateLocation(Location location, String mapboxResponse) {
//        try {
//            // Используем Jackson для парсинга JSON
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode rootNode = objectMapper.readTree(mapboxResponse);
//
//            JsonNode features = rootNode.get("features");
//            if (features != null && features.isArray() && features.size() > 0) {
//
//                // Берем первый результат (самый релевантный)
//                JsonNode firstFeature = features.get(0);
//
//                // Извлекаем основную информацию
//                String placeName = getTextValue(firstFeature, "place_name");
//                String mapboxPlaceId = getTextValue(firstFeature, "id");
//
//                // Обновляем основные поля
//                if (placeName != null) {
//                    location.setAddress(placeName);
//                }
//                if (mapboxPlaceId != null) {
//                    location.setMapboxPlaceId(mapboxPlaceId);
//                }
//
//                // Извлекаем детальную информацию из context
//                JsonNode context = firstFeature.get("context");
//                if (context != null && context.isArray()) {
//                    for (JsonNode contextItem : context) {
//                        String id = getTextValue(contextItem, "id");
//                        String text = getTextValue(contextItem, "text");
//
//                        if (id != null && text != null) {
//                            // Определяем тип по префиксу ID
//                            if (id.startsWith("place.")) {
//                                location.setCity(text);
//                            } else if (id.startsWith("country.")) {
//                                location.setCountry(text);
//                            }
//                        }
//                    }
//                }
//
//                // Если название места не было задано пользователем, используем из Mapbox
//                if (location.getPlaceName() == null || location.getPlaceName().trim().isEmpty()) {
//                    JsonNode properties = firstFeature.get("properties");
//                    if (properties != null) {
//                        String category = getTextValue(properties, "category");
//                        if (category != null) {
//                            location.setPlaceName(category);
//                        }
//                    }
//                }
//
//                log.debug("Парсинг Mapbox ответа завершен. Адрес: {}, Город: {}, Страна: {}",
//                        location.getAddress(), location.getCity(), location.getCountry());
//
//            } else {
//                log.warn("Mapbox не вернул результатов для координат {}, {}",
//                        location.getLatitude(), location.getLongitude());
//            }
//
//        } catch (Exception e) {
//            log.error("Ошибка при парсинге ответа Mapbox: {}", e.getMessage());
//        }
//    }
//
//    private String getTextValue(JsonNode node, String fieldName) {
//        JsonNode field = node.get(fieldName);
//        return (field != null && !field.isNull()) ? field.asText() : null;
//    }
//
//    // Дополнительный метод для получения детальной информации о месте
//    public Location enrichLocationWithPlaceDetails(Location location) {
//        if (location.getMapboxPlaceId() != null) {
//            try {
//                // Запрос детальной информации о месте по Place ID
//                String detailsUrl = String.format("%s/geocoding/v5/mapbox.places/%s.json?access_token=%s",
//                        mapboxService.getBaseUrl(),
//                        location.getMapboxPlaceId(),
//                        mapboxService.getAccessToken());
//
//                // ✅ ИСПРАВЛЕНО: используем RestTemplate из MapboxService
//                RestTemplate restTemplate = mapboxService.getRestTemplate();
//                String response = restTemplate.getForObject(detailsUrl, String.class);
//                if (response != null) {
//                    parseAndUpdateLocation(location, response);
//                }
//
//            } catch (Exception e) {
//                log.warn("Не удалось получить детальную информацию о месте {}: {}",
//                        location.getMapboxPlaceId(), e.getMessage());
//            }
//        }
//        return location;
//    }
//}

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
        // ✅ ДОБАВЛЕНА ВАЛИДАЦИЯ КООРДИНАТ
        if (!isValidCoordinates(latitude, longitude)) {
            throw new IllegalArgumentException("Некорректные координаты: " + latitude + ", " + longitude);
        }

        log.info("Создание локации для координат: {}, {} с названием: '{}'", latitude, longitude, placeName);

        // Проверяем, есть ли уже похожая локация (с погрешностью 100м)
        Optional<Location> existingLocation = locationRepository
                .findByCoordinatesWithPrecision(latitude, longitude, 0.001);

        if (existingLocation.isPresent()) {
            Location location = existingLocation.get();
            log.info("Найдена существующая локация рядом с координатами {}, {}", latitude, longitude);

            // ✅ ПРОВЕРЯЕМ НУЖНО ЛИ ОБНОВИТЬ СУЩЕСТВУЮЩУЮ ЛОКАЦИЮ
            boolean needsUpdate = (location.getAddress() == null || location.getAddress().trim().isEmpty()) &&
                    (location.getPlaceName() == null || location.getPlaceName().trim().isEmpty()) &&
                    (location.getCity() == null || location.getCity().trim().isEmpty()) &&
                    (location.getCountry() == null || location.getCountry().trim().isEmpty());

            if (needsUpdate) {
                log.info("Существующая локация не имеет адресной информации, обновляем...");

                // Устанавливаем placeName если передан
                if (placeName != null && !placeName.trim().isEmpty()) {
                    location.setPlaceName(placeName);
                    log.info("Установлено название из параметра: '{}'", placeName);
                } else {
                    // Устанавливаем временное название
                    location.setPlaceName("Геолокация " + String.format("%.4f, %.4f", latitude, longitude));
                    log.info("Установлено временное название: '{}'", location.getPlaceName());
                }

                // Пытаемся обогатить через Mapbox
                try {
                    enrichLocationWithAddress(location);

                    // Проверяем, получили ли мы данные от Mapbox
                    if (location.getAddress() != null || location.getCity() != null) {
                        log.info("Существующая локация успешно обогащена через Mapbox");
                    } else {
                        log.warn("Mapbox не вернул данных, используем fallback");
                        String fallbackName = generateFallbackLocationName(latitude, longitude);
                        location.setPlaceName(fallbackName);
                        log.info("Установлено fallback название: '{}'", fallbackName);
                    }

                    location = locationRepository.save(location); // Сохраняем обновления
                    log.info("Существующая локация сохранена с обновлениями");

                } catch (Exception e) {
                    log.warn("Не удалось обогатить существующую локацию через Mapbox: {}", e.getMessage());

                    // Устанавливаем fallback название
                    String fallbackName = generateFallbackLocationName(latitude, longitude);
                    location.setPlaceName(fallbackName);
                    location = locationRepository.save(location);
                    log.info("Установлено fallback название для существующей локации: '{}'", fallbackName);
                }
            } else {
                log.info("Существующая локация уже содержит адресную информацию, возвращаем как есть");
            }

            return location;
        }

        // ✅ СОЗДАЕМ НОВУЮ ЛОКАЦИЮ
        log.info("Создаем новую локацию...");
        Location location = Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .placeName(placeName)
                .build();

        // ✅ ДОБАВЛЯЕМ FALLBACK ДЛЯ PLACENAME ЕСЛИ MAPBOX НЕДОСТУПЕН
        if (location.getPlaceName() == null || location.getPlaceName().trim().isEmpty()) {
            // Устанавливаем базовое название до попытки обращения к Mapbox
            location.setPlaceName("Геолокация " + String.format("%.4f, %.4f", latitude, longitude));
            log.info("Установлено временное название для новой локации: '{}'", location.getPlaceName());
        }

        // Опционально: получаем адрес через обратное геокодирование
        try {
            enrichLocationWithAddress(location);

            // Проверяем, получили ли мы данные от Mapbox
            if (location.getAddress() != null || location.getCity() != null) {
                log.info("Новая локация успешно обогащена через Mapbox");
            } else {
                log.warn("Mapbox не вернул данных для новой локации, используем fallback");
                String fallbackName = generateFallbackLocationName(latitude, longitude);
                location.setPlaceName(fallbackName);
                log.info("Установлено fallback название для новой локации: '{}'", fallbackName);
            }

        } catch (Exception e) {
            log.warn("Не удалось получить адрес для новой локации {}, {}: {}",
                    latitude, longitude, e.getMessage());

            // ✅ ЕСЛИ MAPBOX НЕДОСТУПЕН, СОЗДАЕМ ОСМЫСЛЕННОЕ НАЗВАНИЕ
            String fallbackName = generateFallbackLocationName(latitude, longitude);
            location.setPlaceName(fallbackName);
            log.info("Установлено fallback название из-за ошибки Mapbox: '{}'", fallbackName);
        }

        Location savedLocation = locationRepository.save(location);
        log.info("Новая локация сохранена с ID: {}", savedLocation.getId());
        return savedLocation;
    }

    // ✅ ДОБАВЛЯЕМ МЕТОД ДЛЯ СОЗДАНИЯ FALLBACK НАЗВАНИЙ
    private String generateFallbackLocationName(Double latitude, Double longitude) {
        // Простое определение региона по координатам
        if (latitude >= 36.0 && latitude <= 42.0 && longitude >= 26.0 && longitude <= 45.0) {
            return "Турция"; // Приблизительные границы Турции
        } else if (latitude >= 35.0 && latitude <= 38.0 && longitude >= 32.0 && longitude <= 35.0) {
            return "Кипр";
        } else if (latitude >= 39.0 && latitude <= 42.0 && longitude >= 19.0 && longitude <= 30.0) {
            return "Греция";
        } else if (latitude >= 40.0 && latitude <= 70.0 && longitude >= 19.0 && longitude <= 170.0) {
            return "Северная Евразия";
        } else if (latitude >= 25.0 && latitude <= 40.0 && longitude >= -10.0 && longitude <= 40.0) {
            return "Средиземноморье";
        } else {
            return String.format("Координаты %.2f°, %.2f°", latitude, longitude);
        }
    }

    // ✅ ДОБАВЛЕНА ВАЛИДАЦИЯ КООРДИНАТ
    private boolean isValidCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            log.warn("Координаты не могут быть null");
            return false;
        }

        // Проверяем диапазоны
        if (latitude < -90 || latitude > 90) {
            log.warn("Широта вне допустимого диапазона [-90, 90]: {}", latitude);
            return false;
        }
        if (longitude < -180 || longitude > 180) {
            log.warn("Долгота вне допустимого диапазона [-180, 180]: {}", longitude);
            return false;
        }

        // Проверяем, что это не координаты по умолчанию (0,0)
        if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
            log.warn("Координаты слишком близки к (0,0), возможно это значения по умолчанию");
            return false;
        }

        return true;
    }

    private void enrichLocationWithAddress(Location location) {
        try {
            log.debug("Обогащение локации адресной информацией для координат {}, {}",
                    location.getLatitude(), location.getLongitude());

            // Получаем URL для обратного геокодирования
            String reverseGeocodingUrl = mapboxService.getReverseGeocodingUrl(
                    location.getLongitude(), location.getLatitude());

            log.debug("Запрос к Mapbox: {}", reverseGeocodingUrl);

            // Используем RestTemplate из MapboxService
            RestTemplate restTemplate = mapboxService.getRestTemplate();
            String response = restTemplate.getForObject(reverseGeocodingUrl, String.class);

            if (response != null) {
                log.debug("Получен ответ от Mapbox для координат {}, {}",
                        location.getLatitude(), location.getLongitude());

                // Парсим ответ и обновляем локацию
                parseAndUpdateLocation(location, response);

                log.info("Локация успешно обогащена адресной информацией: {}", location.getAddress());
            } else {
                log.warn("Mapbox вернул пустой ответ для координат {}, {}",
                        location.getLatitude(), location.getLongitude());
            }

        } catch (Exception e) {
            log.warn("Не удалось обогатить локацию адресной информацией для координат {}, {}: {}",
                    location.getLatitude(), location.getLongitude(), e.getMessage());
            // Не бросаем исключение - локация может существовать без адреса
        }
    }

    private void parseAndUpdateLocation(Location location, String mapboxResponse) {
        try {
            // ✅ ДОБАВЛЕНО ЛОГИРОВАНИЕ ОТВЕТА MAPBOX
            log.info("Mapbox ответ для координат {}, {}: начинаем парсинг",
                    location.getLatitude(), location.getLongitude());

            // Логируем первые 500 символов ответа для анализа
            log.info("Первые 500 символов ответа Mapbox: {}",
                    mapboxResponse.length() > 500 ? mapboxResponse.substring(0, 500) + "..." : mapboxResponse);

            // Используем Jackson для парсинга JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(mapboxResponse);

            JsonNode features = rootNode.get("features");
            if (features != null && features.isArray() && features.size() > 0) {

                // Берем первый результат (самый релевантный)
                JsonNode firstFeature = features.get(0);

                // Логируем полную структуру первого feature
                log.info("Структура первого feature: {}", firstFeature.toString());

                // Извлекаем основную информацию
                String placeName = getTextValue(firstFeature, "place_name");
                String mapboxPlaceId = getTextValue(firstFeature, "id");

                log.info("Mapbox вернул: place_name='{}', id='{}'", placeName, mapboxPlaceId);

                // Обновляем основные поля
                if (placeName != null) {
                    location.setAddress(placeName);
                }
                if (mapboxPlaceId != null) {
                    location.setMapboxPlaceId(mapboxPlaceId);
                }

                // Извлекаем детальную информацию из context
                JsonNode context = firstFeature.get("context");
                log.info("Context node существует: {}, размер: {}",
                        context != null, context != null ? context.size() : 0);

                if (context != null && context.isArray()) {
                    log.info("Полный context: {}", context.toString());
                    for (JsonNode contextItem : context) {
                        String id = getTextValue(contextItem, "id");
                        String text = getTextValue(contextItem, "text");
                        log.info("Context item - id: '{}', text: '{}'", id, text);

                        if (id != null && text != null) {
                            // Определяем тип по префиксу ID
                            if (id.startsWith("place.")) {
                                location.setCity(text);
                                log.info("✅ Установлен город: '{}'", text);
                            } else if (id.startsWith("country.")) {
                                location.setCountry(text);
                                log.info("✅ Установлена страна: '{}'", text);
                            }
                        }
                    }
                }

                // ✅ УЛУЧШЕННАЯ ЛОГИКА ДЛЯ PLACENAME
                setFallbackPlaceName(location, firstFeature);

                // ✅ ИТОГОВОЕ ЛОГИРОВАНИЕ
                log.info("ИТОГ парсинга: Адрес='{}', Город='{}', Страна='{}', Название='{}'",
                        location.getAddress(), location.getCity(), location.getCountry(), location.getPlaceName());

                // ✅ ПРИНУДИТЕЛЬНО ОБНОВЛЯЕМ PLACENAME ЕСЛИ ОНО ВРЕМЕННОЕ
                if (location.getPlaceName() != null && location.getPlaceName().startsWith("Геолокация")) {
                    log.info("Обнаружено временное placeName, принудительно обновляем...");
                    setFallbackPlaceName(location, firstFeature);
                    log.info("После принудительного обновления placeName: '{}'", location.getPlaceName());
                }

            } else {
                log.warn("Mapbox не вернул результатов для координат {}, {}",
                        location.getLatitude(), location.getLongitude());
            }

        } catch (Exception e) {
            log.error("Ошибка при парсинге ответа Mapbox для координат {}, {}: {}",
                    location.getLatitude(), location.getLongitude(), e.getMessage(), e);
        }
    }

    // ✅ ДОБАВЛЕНА УЛУЧШЕННАЯ ЛОГИКА ДЛЯ PLACENAME
    private void setFallbackPlaceName(Location location, JsonNode firstFeature) {
        log.info("=== НАЧАЛО УСТАНОВКИ PLACENAME ===");
        log.info("Текущее placeName: '{}'", location.getPlaceName());
        log.info("Проверка условия: placeName == null? {}", location.getPlaceName() == null);
        if (location.getPlaceName() != null) {
            log.info("placeName.trim().isEmpty()? {}", location.getPlaceName().trim().isEmpty());
        }

        // Если название места не было задано пользователем или пустое
        if (location.getPlaceName() == null || location.getPlaceName().trim().isEmpty()) {
            log.info("PlaceName пустое или null, ищем альтернативы...");

            // Пытаемся получить категорию из properties
            JsonNode properties = firstFeature.get("properties");
            log.info("Properties node существует: {}", properties != null);
            if (properties != null) {
                String category = getTextValue(properties, "category");
                log.info("Найдена категория: '{}'", category);
                if (category != null && !category.trim().isEmpty()) {
                    location.setPlaceName(category);
                    log.info("✅ Установлено название из категории: '{}'", category);
                    return;
                }
            }

            // Если категории нет, создаем fallback из города и страны
            log.info("Категория не найдена, создаем fallback из города и страны...");
            log.info("Город: '{}', Страна: '{}'", location.getCity(), location.getCountry());

            String fallbackName = "";
            if (location.getCity() != null && !location.getCity().trim().isEmpty()) {
                fallbackName += location.getCity();
                log.info("Добавили город в fallback: '{}'", fallbackName);
            }
            if (location.getCountry() != null && !location.getCountry().trim().isEmpty()) {
                if (!fallbackName.isEmpty()) {
                    fallbackName += ", ";
                }
                fallbackName += location.getCountry();
                log.info("Добавили страну в fallback: '{}'", fallbackName);
            }

            if (!fallbackName.isEmpty()) {
                location.setPlaceName(fallbackName);
                log.info("✅ Установлено fallback название: '{}'", fallbackName);
            } else {
                location.setPlaceName("Неизвестное место");
                log.info("✅ Установлено название по умолчанию: 'Неизвестное место'");
            }
        } else {
            log.info("Название места уже установлено: '{}'", location.getPlaceName());
        }

        log.info("=== ИТОГОВОЕ PLACENAME: '{}' ===", location.getPlaceName());
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

                log.debug("Запрос детальной информации: {}", detailsUrl);

                // Используем RestTemplate из MapboxService
                RestTemplate restTemplate = mapboxService.getRestTemplate();
                String response = restTemplate.getForObject(detailsUrl, String.class);
                if (response != null) {
                    parseAndUpdateLocation(location, response);
                    log.info("Детальная информация о месте {} успешно получена", location.getMapboxPlaceId());
                }

            } catch (Exception e) {
                log.warn("Не удалось получить детальную информацию о месте {}: {}",
                        location.getMapboxPlaceId(), e.getMessage());
            }
        }
        return location;
    }
}