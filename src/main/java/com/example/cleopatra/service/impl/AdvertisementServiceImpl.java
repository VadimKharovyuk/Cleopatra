package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.AdvertisementDTO.*;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.enums.Role;
import com.example.cleopatra.ExistsException.ImageValidationException;
import com.example.cleopatra.model.Advertisement;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.AdvertisementRepository;
import com.example.cleopatra.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdvertisementServiceImpl implements AdvertisementService {

    private final AdvertisementRepository advertisementRepository;
    private final StorageService storageService;
    private final ImageValidator imageValidator;
    private final AdStatisticsService adStatisticsService;

    @Override
    public AdvertisementDetailDTO createAdvertisement(CreateAdvertisementDTO dto, MultipartFile image, User createdBy) throws IOException {
        log.info("Создание рекламы с изображением для пользователя: {}", createdBy.getEmail());

        // Валидация и обработка изображения
        ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(image);

        // Загрузка изображения
        StorageService.StorageResult storageResult = storageService.uploadProcessedImage(processedImage);

        // Создание рекламы
        Advertisement advertisement = buildAdvertisementFromDTO(dto, createdBy);
        advertisement.setImageUrl(storageResult.getUrl());
        advertisement.setImgId(storageResult.getImageId());

        Advertisement savedAd = advertisementRepository.save(advertisement);

        log.info("Реклама успешно создана с ID: {}", savedAd.getId());
        return AdvertisementDetailDTO.fromEntity(savedAd);
    }

    @Override
    public AdvertisementDetailDTO createAdvertisement(CreateAdvertisementDTO dto, User createdBy) {
        log.info("Создание рекламы без изображения для пользователя: {}", createdBy.getEmail());

        Advertisement advertisement = buildAdvertisementFromDTO(dto, createdBy);
        Advertisement savedAd = advertisementRepository.save(advertisement);

        log.info("Реклама успешно создана с ID: {}", savedAd.getId());
        return AdvertisementDetailDTO.fromEntity(savedAd);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdvertisementDetailDTO> getAdvertisementDetails(Long id, User requestingUser) {
        log.debug("Получение деталей рекламы {} для пользователя {}", id, requestingUser.getEmail());

        return advertisementRepository.findById(id)
                .filter(ad -> canViewDetails(ad, requestingUser))
                .map(AdvertisementDetailDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdvertisementResponseDTO> getAdvertisementForDisplay(Long id) {
        return advertisementRepository.findById(id)
                .filter(ad -> ad.getStatus() == AdStatus.ACTIVE)
                .map(AdvertisementResponseDTO::fromEntity);
    }

    @Override
    public AdvertisementDetailDTO updateAdvertisement(Long id, UpdateAdvertisementDTO dto, User requestingUser) {
        log.info("Обновление рекламы {} пользователем {}", id, requestingUser.getEmail());

        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        if (!canModify(advertisement, requestingUser)) {
            throw new RuntimeException("Нет прав для редактирования этой рекламы");
        }

        updateAdvertisementFields(advertisement, dto);
        Advertisement savedAd = advertisementRepository.save(advertisement);

        log.info("Реклама {} успешно обновлена", id);
        return AdvertisementDetailDTO.fromEntity(savedAd);
    }

    @Override
    public AdvertisementDetailDTO updateAdvertisementImage(Long id, MultipartFile newImage, User requestingUser) throws IOException {
        log.info("Обновление изображения рекламы {} пользователем {}", id, requestingUser.getEmail());

        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        if (!canModify(advertisement, requestingUser)) {
            throw new RuntimeException("Нет прав для редактирования этой рекламы");
        }

        // Удаляем старое изображение если есть
        if (advertisement.getImgId() != null) {
            storageService.deleteImage(advertisement.getImgId());
        }

        // Загружаем новое изображение
        ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(newImage);
        StorageService.StorageResult storageResult = storageService.uploadProcessedImage(processedImage);

        advertisement.setImageUrl(storageResult.getUrl());
        advertisement.setImgId(storageResult.getImageId());
        advertisement.setUpdatedAt(LocalDateTime.now());

        Advertisement savedAd = advertisementRepository.save(advertisement);

        log.info("Изображение рекламы {} успешно обновлено", id);
        return AdvertisementDetailDTO.fromEntity(savedAd);
    }

    @Override
    public boolean deleteAdvertisement(Long id, User requestingUser) {
        log.info("Удаление рекламы {} пользователем {}", id, requestingUser.getEmail());

        Optional<Advertisement> adOptional = advertisementRepository.findById(id);
        if (adOptional.isEmpty()) {
            return false;
        }

        Advertisement advertisement = adOptional.get();
        if (!canModify(advertisement, requestingUser)) {
            throw new RuntimeException("Нет прав для удаления этой рекламы");
        }

        // Удаляем изображение если есть
        if (advertisement.getImgId() != null) {
            storageService.deleteImage(advertisement.getImgId());
        }

        advertisementRepository.delete(advertisement);
        log.info("Реклама {} успешно удалена", id);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdvertisementResponseDTO> getRandomActiveAdvertisement(User user) {
        log.debug("Получение случайной рекламы для пользователя: {}",
                user != null ? user.getEmail() : "анонимный");

        try {
            // Сначала пробуем получить с учетом всех параметров пользователя
            if (user != null) {
                Optional<AdvertisementResponseDTO> targetedAd = getTargetedAdvertisement(user);
                if (targetedAd.isPresent()) {
                    return targetedAd;
                }
            }

            // Если не получилось или пользователь = null, получаем любую активную
            return getAnyActiveAdvertisement();

        } catch (Exception e) {
            log.error("Ошибка получения случайной рекламы: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AdvertisementResponseDTO> getTargetedAdvertisement(User user) {
        try {
            Gender userGender = user.getGender();
            String userCity = user.getCity();

            Pageable pageable = PageRequest.of(0, 1);

            return advertisementRepository.findActiveAdsForUser(
                            userGender,
                            userCity,
                            pageable
                    ).getContent().stream()
                    .findFirst()
                    .map(AdvertisementResponseDTO::fromEntity);

        } catch (Exception e) {
            log.warn("Ошибка получения таргетированной рекламы: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AdvertisementResponseDTO> getAnyActiveAdvertisement() {
        try {
            // Получаем все активные объявления
            List<Advertisement> activeAds = advertisementRepository.findByStatus(AdStatus.ACTIVE);

            if (activeAds.isEmpty()) {
                log.debug("Активные объявления не найдены");
                return Optional.empty();
            }

            // Фильтруем по времени и дате (если нужно)
            List<Advertisement> availableAds = activeAds.stream()
                    .filter(this::isAdCurrentlyActive)
                    .toList();

            if (availableAds.isEmpty()) {
                log.debug("Нет доступных для показа объявлений");
                return Optional.empty();
            }

            // Выбираем случайное
            Advertisement randomAd = availableAds.get(
                    new java.util.Random().nextInt(availableAds.size())
            );

            log.debug("Выбрано объявление: {} (ID: {})", randomAd.getTitle(), randomAd.getId());

            return Optional.of(AdvertisementResponseDTO.fromEntity(randomAd));

        } catch (Exception e) {
            log.error("Ошибка получения любой активной рекламы: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isAdCurrentlyActive(Advertisement ad) {
        LocalTime currentTime = LocalTime.now();
        LocalDate currentDate = LocalDate.now();

        // Проверка даты окончания
        if (ad.getEndDate() != null && ad.getEndDate().isBefore(currentDate)) {
            return false;
        }

        // Проверка времени начала
        if (ad.getStartTime() != null && ad.getStartTime().isAfter(currentTime)) {
            return false;
        }

        // Проверка времени окончания
        if (ad.getEndTime() != null && ad.getEndTime().isBefore(currentTime)) {
            return false;
        }

        // Проверка бюджета
        if (ad.getRemainingBudget() != null &&
                ad.getCostPerView() != null &&
                ad.getRemainingBudget().compareTo(ad.getCostPerView()) < 0) {
            return false;
        }

        return true;
    }

    @Override
    public void registerView(Long advertisementId, User viewer) {
        log.debug("Регистрация просмотра рекламы {} пользователем {}", advertisementId, viewer.getEmail());

        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        // Проверяем бюджет
        if (advertisement.getRemainingBudget().compareTo(advertisement.getCostPerView()) < 0) {
            advertisement.setStatus(AdStatus.FINISHED);
            advertisementRepository.save(advertisement);
            return;
        }

        // Списываем средства и увеличиваем счетчик
        BigDecimal newBudget = advertisement.getRemainingBudget().subtract(advertisement.getCostPerView());
        advertisement.setRemainingBudget(newBudget);
        advertisement.setViewsCount(advertisement.getViewsCount() + 1);
        advertisement.setLastViewedAt(LocalDateTime.now());

        // Проверяем не закончился ли бюджет
        if (newBudget.compareTo(advertisement.getCostPerView()) < 0) {
            advertisement.setStatus(AdStatus.FINISHED);
        }

        advertisementRepository.save(advertisement);

        // Записываем в детальную статистику (если сервис доступен)
        try {
            adStatisticsService.recordView(advertisementId, viewer, null);
        } catch (Exception e) {
            log.warn("Ошибка записи детальной статистики просмотра: {}", e.getMessage());
        }
    }

    @Override
    public void registerClick(Long advertisementId, User clicker) {
        log.debug("Регистрация клика по рекламе {} пользователем {}", advertisementId, clicker.getEmail());

        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        // Проверяем бюджет для клика
        if (advertisement.getRemainingBudget().compareTo(advertisement.getCostPerClick()) < 0) {
            return;
        }

        // Списываем средства и увеличиваем счетчик
        BigDecimal newBudget = advertisement.getRemainingBudget().subtract(advertisement.getCostPerClick());
        advertisement.setRemainingBudget(newBudget);
        advertisement.setClicksCount(advertisement.getClicksCount() + 1);
        advertisement.setLastClickedAt(LocalDateTime.now());

        // Проверяем не закончился ли бюджет
        if (newBudget.compareTo(advertisement.getCostPerView()) < 0) {
            advertisement.setStatus(AdStatus.FINISHED);
        }

        advertisementRepository.save(advertisement);

        // Записываем в детальную статистику
        try {
            adStatisticsService.recordClick(advertisementId, clicker, null);
        } catch (Exception e) {
            log.warn("Ошибка записи детальной статистики клика: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(Long advertisementId, User user) {
        return advertisementRepository.findById(advertisementId)
                .map(ad -> ad.getCreatedBy().getId().equals(user.getId()))
                .orElse(false);
    }

    @Override
    public void approveAdvertisement(Long advertisementId, User admin) {
        log.info("Одобрение рекламы {} администратором {}", advertisementId, admin.getEmail());

        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        if (advertisement.getStatus() != AdStatus.PENDING) {
            throw new RuntimeException("Можно одобрить только рекламу со статусом 'На модерации'");
        }

        advertisement.setStatus(AdStatus.ACTIVE);
        advertisement.setApprovedBy(admin);
        advertisement.setApprovedAt(LocalDateTime.now());
        advertisement.setUpdatedAt(LocalDateTime.now());

        advertisementRepository.save(advertisement);

        log.info("Реклама {} успешно одобрена", advertisementId);

        // Можно добавить отправку уведомления создателю рекламы
        // notificationService.sendApprovalNotification(advertisement.getCreatedBy(), advertisement);
    }

    @Override
    public void rejectAdvertisement(Long advertisementId, String reason, String comment, User admin) {
        log.info("Отклонение рекламы {} администратором {}, причина: {}",
                advertisementId, admin.getEmail(), reason);

        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        if (advertisement.getStatus() != AdStatus.PENDING) {
            throw new RuntimeException("Можно отклонить только рекламу со статусом 'На модерации'");
        }

        advertisement.setStatus(AdStatus.REJECTED);
        advertisement.setApprovedBy(admin);
        advertisement.setRejectionReason(reason);
        if (comment != null && !comment.trim().isEmpty()) {
            advertisement.setAdminNotes(comment);
        }
        advertisement.setUpdatedAt(LocalDateTime.now());

        advertisementRepository.save(advertisement);

        log.info("Реклама {} отклонена", advertisementId);

        // Можно добавить отправку уведомления с причиной отклонения
        // notificationService.sendRejectionNotification(advertisement.getCreatedBy(), advertisement, reason, comment);
    }

    @Override
    public void pauseAdvertisement(Long advertisementId, User admin) {
        log.info("Приостановка рекламы {} администратором {}", advertisementId, admin.getEmail());

        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        if (advertisement.getStatus() != AdStatus.ACTIVE) {
            throw new RuntimeException("Можно приостановить только активную рекламу");
        }

        advertisement.setStatus(AdStatus.PAUSED);
        advertisement.setUpdatedAt(LocalDateTime.now());

        advertisementRepository.save(advertisement);

        log.info("Реклама {} приостановлена", advertisementId);
    }

    @Override
    public void blockAdvertisement(Long advertisementId, String reason, String comment, User admin) {
        log.info("Блокировка рекламы {} администратором {}, причина: {}",
                advertisementId, admin.getEmail(), reason);

        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        advertisement.setStatus(AdStatus.REJECTED); // или создайте статус BLOCKED
        advertisement.setRejectionReason(reason);
        if (comment != null && !comment.trim().isEmpty()) {
            advertisement.setAdminNotes(comment);
        }
        advertisement.setUpdatedAt(LocalDateTime.now());

        advertisementRepository.save(advertisement);

        log.info("Реклама {} заблокирована", advertisementId);

        // Уведомление о блокировке
        // notificationService.sendBlockNotification(advertisement.getCreatedBy(), advertisement, reason, comment);
    }

    @Override
    public void dismissReports(Long advertisementId, User admin) {
        log.info("Отклонение жалоб для рекламы {} администратором {}", advertisementId, admin.getEmail());

        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        // Удаляем или помечаем жалобы как рассмотренные
        if (advertisementRepository != null) {
            // adReportRepository.dismissReportsForAdvertisement(advertisementId, admin);
        }

        log.info("Жалобы для рекламы {} отклонены как необоснованные", advertisementId);
    }

    // Приватные методы-помощники

    private Advertisement buildAdvertisementFromDTO(CreateAdvertisementDTO dto, User createdBy) {
        LocalDateTime now = LocalDateTime.now();

        return Advertisement.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .shortDescription(dto.getShortDescription())
                .url(dto.getUrl())
                .totalBudget(dto.getTotalBudget())
                .remainingBudget(dto.getTotalBudget()) // Изначально равен общему бюджету
                .costPerView(dto.getCostPerView() != null ? dto.getCostPerView() : new BigDecimal("0.20"))
                .costPerClick(dto.getCostPerClick() != null ? dto.getCostPerClick() : new BigDecimal("0.50"))
                .targetGender(dto.getTargetGender())
                .minAge(dto.getMinAge())
                .maxAge(dto.getMaxAge())
                .targetCity(dto.getTargetCity())
                .category(dto.getCategory())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(AdStatus.PENDING) // Требует модерации
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void updateAdvertisementFields(Advertisement advertisement, UpdateAdvertisementDTO dto) {
        if (dto.getTitle() != null) {
            advertisement.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            advertisement.setDescription(dto.getDescription());
        }
        if (dto.getShortDescription() != null) {
            advertisement.setShortDescription(dto.getShortDescription());
        }
        if (dto.getUrl() != null) {
            advertisement.setUrl(dto.getUrl());
        }
        if (dto.getAdditionalBudget() != null && dto.getAdditionalBudget().compareTo(BigDecimal.ZERO) > 0) {
            advertisement.setTotalBudget(advertisement.getTotalBudget().add(dto.getAdditionalBudget()));
            advertisement.setRemainingBudget(advertisement.getRemainingBudget().add(dto.getAdditionalBudget()));
        }
        if (dto.getStartTime() != null) {
            advertisement.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            advertisement.setEndTime(dto.getEndTime());
        }
        if (dto.getEndDate() != null) {
            advertisement.setEndDate(dto.getEndDate());
        }

        advertisement.setUpdatedAt(LocalDateTime.now());
    }

    private boolean canViewDetails(Advertisement advertisement, User user) {
        // Владелец может видеть всё
        if (advertisement.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Админ может видеть всё
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        // Остальные только активные рекламы
        return advertisement.getStatus() == AdStatus.ACTIVE;
    }

    private boolean canModify(Advertisement advertisement, User user) {
        // Владелец может редактировать
        if (advertisement.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Админ может редактировать любую
        return user.getRole() == Role.ADMIN;
    }

    private Integer calculateUserAge(User user) {
        // Если у вас есть дата рождения в User, вычислите возраст
        // Пока возвращаем null
        return null;
    }
}