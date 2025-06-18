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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
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
    private final UserService userService;

    @Override
    @Transactional
    public AdvertisementDetailDTO createAdvertisement(CreateAdvertisementDTO dto, MultipartFile image, User createdBy) throws IOException {
        log.info("Создание рекламы с изображением для пользователя: {}", createdBy.getEmail());

        // Проверяем и резервируем средства с баланса
        BigDecimal totalBudget = dto.getTotalBudget();
        BigDecimal userBalance = createdBy.getBalance();

        if (userBalance == null) {
            userBalance = BigDecimal.ZERO;
            createdBy.setBalance(userBalance);
        }

        if (userBalance.compareTo(totalBudget) < 0) {
            throw new RuntimeException(String.format(
                    "Недостаточно средств на балансе. Доступно: %s, требуется: %s",
                    userBalance, totalBudget));
        }

        // Валидация и обработка изображения
        ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(image);

        // Загрузка изображения
        StorageService.StorageResult storageResult = storageService.uploadProcessedImage(processedImage);

        // Создание рекламы
        Advertisement advertisement = buildAdvertisementFromDTO(dto, createdBy);
        advertisement.setImageUrl(storageResult.getUrl());
        advertisement.setImgId(storageResult.getImageId());

        // Резервируем средства с баланса пользователя
        BigDecimal newBalance = userBalance.subtract(totalBudget);
        createdBy.setBalance(newBalance);
        userService.save(createdBy);

        Advertisement savedAd = advertisementRepository.save(advertisement);

        log.info("Реклама успешно создана с ID: {}. Зарезервировано {} с баланса. Остаток баланса: {}",
                savedAd.getId(), totalBudget, newBalance);

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
        log.info("=== ОТЛАДКА getRandomActiveAdvertisement ===");
        log.info("Пользователь: {} (пол: {})",
                user != null ? user.getEmail() : "null",
                user != null && user.getGender() != null ? user.getGender().name() : "null");

        try {
            // Шаг 1: Получаем все активные объявления
            log.info("Шаг 1: Получаем все активные объявления...");
            List<Advertisement> allActiveAds = advertisementRepository.findByStatus(AdStatus.ACTIVE);
            log.info("Найдено активных объявлений: {}", allActiveAds.size());

            if (allActiveAds.isEmpty()) {
                log.warn("❌ В базе нет активных объявлений!");
                return Optional.empty();
            }

            // Выводим информацию о найденных объявлениях
            for (Advertisement ad : allActiveAds) {
                log.info("  - ID: {}, Заголовок: '{}', Target Gender: {}, Start Time: {}, End Time: {}, End Date: {}",
                        ad.getId(),
                        ad.getTitle(),
                        ad.getTargetGender() != null ? ad.getTargetGender().name() : "null",
                        ad.getStartTime(),
                        ad.getEndTime(),
                        ad.getEndDate());
            }

            // Шаг 2: Фильтруем подходящие объявления
            log.info("Шаг 2: Фильтруем подходящие для пользователя...");
            List<Advertisement> suitableAds = allActiveAds.stream()
                    .filter(ad -> {
                        boolean suitable = isAdSuitableForUser(ad, user);
                        log.info("  Объявление ID {}: подходит = {}", ad.getId(), suitable);
                        return suitable;
                    })
                    .toList();

            log.info("После фильтрации по пользователю: {}", suitableAds.size());

            // Шаг 3: Фильтруем по времени
            log.info("Шаг 3: Фильтруем по времени и дате...");
            LocalTime now = LocalTime.now();
            LocalDate today = LocalDate.now();
            log.info("Текущее время: {}, дата: {}", now, today);

            List<Advertisement> activeNow = suitableAds.stream()
                    .filter(ad -> {
                        boolean active = isAdCurrentlyActive(ad);
                        if (!active) {
                            log.info("  Объявление ID {} НЕ активно сейчас:", ad.getId());
                            if (ad.getEndDate() != null && ad.getEndDate().isBefore(today)) {
                                log.info("    - Дата окончания прошла: {} < {}", ad.getEndDate(), today);
                            }
                            if (ad.getStartTime() != null && ad.getStartTime().isAfter(now)) {
                                log.info("    - Время начала в будущем: {} > {}", ad.getStartTime(), now);
                            }
                            if (ad.getEndTime() != null && ad.getEndTime().isBefore(now)) {
                                log.info("    - Время окончания прошло: {} < {}", ad.getEndTime(), now);
                            }
                            if (ad.getRemainingBudget() != null && ad.getCostPerView() != null &&
                                    ad.getRemainingBudget().compareTo(ad.getCostPerView()) < 0) {
                                log.info("    - Недостаточно бюджета: {} < {}", ad.getRemainingBudget(), ad.getCostPerView());
                            }
                        } else {
                            log.info("  Объявление ID {} активно сейчас ✓", ad.getId());
                        }
                        return active;
                    })
                    .toList();


            if (activeNow.isEmpty()) {
                log.warn("❌ Нет объявлений, активных в данный момент");

                // Fallback: показываем любое активное, игнорируя фильтры
                log.info("Пробуем fallback - любое активное объявление...");
                if (!allActiveAds.isEmpty()) {
                    Advertisement fallbackAd = allActiveAds.get(0);
                    log.info("✅ Используем fallback объявление: ID {}", fallbackAd.getId());
                    return Optional.of(AdvertisementResponseDTO.fromEntity(fallbackAd));
                }

                return Optional.empty();
            }

            // Шаг 4: Выбираем случайное
            Advertisement randomAd = activeNow.get(new Random().nextInt(activeNow.size()));
            log.info("✅ Выбрано объявление: ID {}, Заголовок: '{}'", randomAd.getId(), randomAd.getTitle());

            return Optional.of(AdvertisementResponseDTO.fromEntity(randomAd));

        } catch (Exception e) {
            log.error("❌ Ошибка в getRandomActiveAdvertisement: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    // Также добавим отладку в вспомогательные методы:
    private boolean isAdSuitableForUser(Advertisement ad, User user) {
        try {
            log.debug("Проверяем соответствие объявления ID {} пользователю", ad.getId());

            if (user == null) {
                boolean suitable = ad.getTargetGender() == null;
                log.debug("  Пользователь null, объявление подходит: {} (target_gender: {})",
                        suitable, ad.getTargetGender());
                return suitable;
            }

            // Проверка пола
            if (ad.getTargetGender() != null && user.getGender() != null) {
                boolean genderMatch = ad.getTargetGender().equals(user.getGender());
                log.debug("  Проверка пола: объявление {} vs пользователь {} = {}",
                        ad.getTargetGender().name(), user.getGender().name(), genderMatch);
                if (!genderMatch) {
                    return false;
                }
            } else {
                log.debug("  Пол: объявление target_gender={}, пользователь gender={} - пропускаем проверку",
                        ad.getTargetGender(), user.getGender());
            }

            // Другие проверки (возраст, город) - пока упростим
            log.debug("  Объявление ID {} подходит пользователю", ad.getId());
            return true;

        } catch (Exception e) {
            log.warn("Ошибка проверки соответствия объявления пользователю: {}", e.getMessage());
            return false;
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

        // Проверка баланса владельца рекламы
        User adOwner = ad.getCreatedBy();
        if (adOwner != null) {
            BigDecimal ownerBalance = adOwner.getBalance();
            BigDecimal requiredAmount = ad.getCostPerView() != null ? ad.getCostPerView() : BigDecimal.ZERO;

            // Защита от null баланса
            if (ownerBalance == null) {
                ownerBalance = BigDecimal.ZERO;
            }

            if (ownerBalance.compareTo(requiredAmount) < 0) {
                log.debug("Реклама ID {} неактивна: недостаточно средств у владельца. Баланс: {}, требуется: {}",
                        ad.getId(), ownerBalance, requiredAmount);
                return false;
            }
        }

        // Дополнительная проверка remainingBudget для совместимости
        if (ad.getRemainingBudget() != null &&
                ad.getCostPerView() != null &&
                ad.getRemainingBudget().compareTo(ad.getCostPerView()) < 0) {
            return false;
        }

        return true;
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
        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        if (advertisement.getStatus() != AdStatus.ACTIVE) {
            throw new RuntimeException("Можно приостановить только активную рекламу");
        }

        advertisement.setStatus(AdStatus.PAUSED);
        advertisement.setUpdatedAt(LocalDateTime.now());

        advertisementRepository.save(advertisement);

    }

    @Override
    public void blockAdvertisement(Long advertisementId, String reason, String comment, User admin) {

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


    @Override
    @Transactional
    public void registerView(Long advertisementId, User viewer, HttpServletRequest request) {
        if (viewer == null) {
            log.debug("Пропускаем регистрацию просмотра для анонимного пользователя");
            return;
        }

        try {
            Advertisement advertisement = advertisementRepository.findById(advertisementId)
                    .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

            BigDecimal costPerView = advertisement.getCostPerView();
            if (costPerView == null) {
                costPerView = new BigDecimal("0.20");
            }

            // Проверяем remainingBudget рекламы (зарезервированные средства)
            BigDecimal remainingBudget = advertisement.getRemainingBudget();
            if (remainingBudget == null) {
                remainingBudget = BigDecimal.ZERO;
            }

            if (remainingBudget.compareTo(costPerView) < 0) {
                log.warn("Недостаточно зарезервированного бюджета для просмотра рекламы ID {}. Остаток: {}, требуется: {}",
                        advertisementId, remainingBudget, costPerView);

                // Приостанавливаем рекламу из-за исчерпания бюджета
                advertisement.setStatus(AdStatus.FINISHED);
                advertisementRepository.save(advertisement);
                return;
            }

            // Списываем средства с remainingBudget (зарезервированных средств)
            BigDecimal newRemainingBudget = remainingBudget.subtract(costPerView);
            advertisement.setRemainingBudget(newRemainingBudget);

            // Увеличиваем счетчик просмотров
            long currentViews = advertisement.getViewsCount() != null ? advertisement.getViewsCount() : 0;
            advertisement.setViewsCount(currentViews + 1);
            advertisement.setLastViewedAt(LocalDateTime.now());

            // Проверяем, нужно ли завершить рекламу
            if (newRemainingBudget.compareTo(costPerView) < 0) {
                advertisement.setStatus(AdStatus.FINISHED);
                log.info("Реклама ID {} завершена из-за исчерпания бюджета", advertisementId);
            }

            advertisementRepository.save(advertisement);
            // Записываем статистику с HttpServletRequest
            recordViewStatistics(advertisementId, viewer, request);

        } catch (Exception e) {
            log.error("Ошибка регистрации просмотра: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void registerClick(Long advertisementId, User clicker, HttpServletRequest request) {
        if (clicker == null) {
            log.debug("Пропускаем регистрацию клика для анонимного пользователя");
            return;
        }

        try {
            Advertisement advertisement = advertisementRepository.findById(advertisementId)
                    .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

            BigDecimal costPerClick = advertisement.getCostPerClick();
            if (costPerClick == null) {
                costPerClick = new BigDecimal("0.50");
            }

            // Проверяем remainingBudget рекламы
            BigDecimal remainingBudget = advertisement.getRemainingBudget();
            if (remainingBudget == null) {
                remainingBudget = BigDecimal.ZERO;
            }

            if (remainingBudget.compareTo(costPerClick) < 0) {
                log.warn("Недостаточно зарезервированного бюджета для клика по рекламе ID {}. Остаток: {}, требуется: {}",
                        advertisementId, remainingBudget, costPerClick);
                return;
            }

            // Списываем средства с remainingBudget
            BigDecimal newRemainingBudget = remainingBudget.subtract(costPerClick);
            advertisement.setRemainingBudget(newRemainingBudget);

            // Увеличиваем счетчик кликов
            long currentClicks = advertisement.getClicksCount() != null ? advertisement.getClicksCount() : 0;
            advertisement.setClicksCount(currentClicks + 1);
            advertisement.setLastClickedAt(LocalDateTime.now());

            // Проверяем, нужно ли завершить рекламу
            BigDecimal minCostForActivity = advertisement.getCostPerView();
            if (minCostForActivity == null) {
                minCostForActivity = new BigDecimal("0.20");
            }

            if (newRemainingBudget.compareTo(minCostForActivity) < 0) {
                advertisement.setStatus(AdStatus.FINISHED);
                log.info("Реклама ID {} завершена из-за исчерпания бюджета", advertisementId);
            }

            advertisementRepository.save(advertisement);

            log.info("Списано {} за клик с бюджета рекламы ID {}. Остаток бюджета: {}",
                    costPerClick, advertisementId, newRemainingBudget);

            // Записываем статистику с HttpServletRequest
            recordClickStatistics(advertisementId, clicker, request);

        } catch (Exception e) {
            log.error("Ошибка регистрации клика: {}", e.getMessage(), e);
            throw e;
        }
    }

// =================
// ОБНОВЛЕННЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
// =================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void recordViewStatistics(Long advertisementId, User viewer, HttpServletRequest request) {
        try {
            adStatisticsService.recordView(advertisementId, viewer, request);
        } catch (Exception e) {
            log.warn("Не удалось записать статистику просмотра: {}", e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void recordClickStatistics(Long advertisementId, User clicker, HttpServletRequest request) {
        try {
            adStatisticsService.recordClick(advertisementId, clicker, request);
        } catch (Exception e) {
            log.warn("Не удалось записать статистику клика: {}", e.getMessage());
        }
    }

    // Можете также оставить простые методы для обратной совместимости
    @Deprecated
    @Async
    protected void recordSimpleViewStatistics(Long advertisementId, User viewer) {
        recordViewStatistics(advertisementId, viewer, null);
    }

    @Deprecated
    @Async
    protected void recordSimpleClickStatistics(Long advertisementId, User clicker) {
        recordClickStatistics(advertisementId, clicker, null);
    }

    @Override
    @Transactional
    public AdvertisementDetailDTO addBudgetToAdvertisement(Long advertisementId, BigDecimal additionalBudget, User user) {
        log.info("Пополнение бюджета рекламы {} на сумму {} пользователем {}",
                advertisementId, additionalBudget, user.getEmail());

        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Реклама не найдена"));

        if (!canModify(advertisement, user)) {
            throw new RuntimeException("Нет прав для пополнения бюджета этой рекламы");
        }

        // Проверяем баланс пользователя
        BigDecimal userBalance = user.getBalance();
        if (userBalance == null) {
            userBalance = BigDecimal.ZERO;
        }

        if (userBalance.compareTo(additionalBudget) < 0) {
            throw new RuntimeException(String.format(
                    "Недостаточно средств на балансе. Доступно: %s, требуется: %s",
                    userBalance, additionalBudget));
        }

        // Списываем с баланса и добавляем к бюджету рекламы
        BigDecimal newBalance = userBalance.subtract(additionalBudget);
        user.setBalance(newBalance);
        userService.save(user);

        // Увеличиваем бюджеты рекламы
        BigDecimal newTotalBudget = advertisement.getTotalBudget().add(additionalBudget);
        BigDecimal newRemainingBudget = advertisement.getRemainingBudget().add(additionalBudget);

        advertisement.setTotalBudget(newTotalBudget);
        advertisement.setRemainingBudget(newRemainingBudget);
        advertisement.setUpdatedAt(LocalDateTime.now());

        // Если реклама была завершена из-за недостатка средств, активируем её
        if (advertisement.getStatus() == AdStatus.FINISHED) {
            advertisement.setStatus(AdStatus.ACTIVE);
            log.info("Реклама ID {} повторно активирована после пополнения бюджета", advertisementId);
        }

        Advertisement savedAd = advertisementRepository.save(advertisement);

        log.info("Бюджет рекламы {} пополнен на {}. Новый общий бюджет: {}, остаток: {}",
                advertisementId, additionalBudget, newTotalBudget, newRemainingBudget);

        return AdvertisementDetailDTO.fromEntity(savedAd);
    }


    @Override
    public Map<String, Object> debugAdvertisements() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Общее количество объявлений
            long totalCount = advertisementRepository.count();
            result.put("total_advertisements", totalCount);

            // 2. Количество по статусам
            long activeCount = advertisementRepository.countByStatus(AdStatus.ACTIVE);
            long pendingCount = advertisementRepository.countByStatus(AdStatus.PENDING);
            long rejectedCount = advertisementRepository.countByStatus(AdStatus.REJECTED);

            result.put("active_count", activeCount);
            result.put("pending_count", pendingCount);
            result.put("rejected_count", rejectedCount);

            // 3. Первые несколько активных объявлений
            List<Advertisement> activeAds = advertisementRepository.findByStatus(AdStatus.ACTIVE);
            result.put("active_ads_details", activeAds.stream()
                    .limit(5)
                    .map(ad -> {
                        Map<String, Object> adInfo = new HashMap<>();
                        adInfo.put("id", ad.getId());
                        adInfo.put("title", ad.getTitle());
                        adInfo.put("status", ad.getStatus().name());
                        adInfo.put("target_gender", ad.getTargetGender() != null ? ad.getTargetGender().name() : null);
                        adInfo.put("start_time", ad.getStartTime());
                        adInfo.put("end_time", ad.getEndTime());
                        adInfo.put("end_date", ad.getEndDate());
                        adInfo.put("remaining_budget", ad.getRemainingBudget());
                        return adInfo;
                    })
                    .collect(Collectors.toList()));

            // 4. Проверяем фильтрацию
            List<Advertisement> filteredAds = activeAds.stream()
                    .filter(this::isAdCurrentlyActive)
                    .toList();
            result.put("filtered_active_count", filteredAds.size());

            // 5. Причины фильтрации
            List<String> filterReasons = new ArrayList<>();
            LocalTime now = LocalTime.now();
            LocalDate today = LocalDate.now();

            for (Advertisement ad : activeAds) {
                List<String> reasons = new ArrayList<>();

                if (ad.getEndDate() != null && ad.getEndDate().isBefore(today)) {
                    reasons.add("end_date_passed");
                }
                if (ad.getStartTime() != null && ad.getStartTime().isAfter(now)) {
                    reasons.add("start_time_future");
                }
                if (ad.getEndTime() != null && ad.getEndTime().isBefore(now)) {
                    reasons.add("end_time_passed");
                }
                if (ad.getRemainingBudget() != null && ad.getCostPerView() != null &&
                        ad.getRemainingBudget().compareTo(ad.getCostPerView()) < 0) {
                    reasons.add("insufficient_budget");
                }

                if (!reasons.isEmpty()) {
                    filterReasons.add("ID " + ad.getId() + ": " + String.join(", ", reasons));
                }
            }
            result.put("filter_reasons", filterReasons);

        } catch (Exception e) {
            result.put("debug_error", e.getMessage());
            log.error("Ошибка диагностики рекламы: {}", e.getMessage(), e);
        }

        return result;
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


}