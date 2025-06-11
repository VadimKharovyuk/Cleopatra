package com.example.cleopatra.service;
import com.example.cleopatra.dto.AdvertisementDTO.CreateAdvertisementDTO;
import com.example.cleopatra.dto.AdvertisementDTO.AdvertisementDetailDTO;
import com.example.cleopatra.dto.AdvertisementDTO.AdvertisementResponseDTO;
import com.example.cleopatra.dto.AdvertisementDTO.UpdateAdvertisementDTO;
import com.example.cleopatra.model.Advertisement;
import com.example.cleopatra.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface AdvertisementService {

    /**
     * Создает новую рекламу с изображением
     */
    AdvertisementDetailDTO createAdvertisement(CreateAdvertisementDTO dto, MultipartFile image, User createdBy) throws IOException;

    /**
     * Создает новую рекламу без изображения
     */
    AdvertisementDetailDTO createAdvertisement(CreateAdvertisementDTO dto, User createdBy);

    /**
     * Получает рекламу по ID для владельца (полная информация)
     */
    Optional<AdvertisementDetailDTO> getAdvertisementDetails(Long id, User requestingUser);

    /**
     * Получает рекламу для показа пользователю (публичная информация)
     */
    Optional<AdvertisementResponseDTO> getAdvertisementForDisplay(Long id);

    /**
     * Обновляет рекламу
     */
    AdvertisementDetailDTO updateAdvertisement(Long id, UpdateAdvertisementDTO dto, User requestingUser);

    /**
     * Обновляет изображение рекламы
     */
    AdvertisementDetailDTO updateAdvertisementImage(Long id, MultipartFile newImage, User requestingUser) throws IOException;

    /**
     * Удаляет рекламу (только владелец или админ)
     */
    boolean deleteAdvertisement(Long id, User requestingUser);

    /**
     * Получает случайную активную рекламу для показа пользователю
     */
    Optional<AdvertisementResponseDTO> getRandomActiveAdvertisement(User user);

    /**
     * Регистрирует просмотр рекламы
     */
    void registerView(Long advertisementId, User viewer);

    /**
     * Регистрирует клик по рекламе
     */
    void registerClick(Long advertisementId, User clicker);

    /**
     * Проверяет, принадлежит ли реклама пользователю
     */
    boolean isOwner(Long advertisementId, User user);



    void approveAdvertisement(Long advertisementId, User admin);

    /**
     * Отклоняет рекламу
     */
    void rejectAdvertisement(Long advertisementId, String reason, String comment, User admin);

    /**
     * Приостанавливает рекламу
     */
    void pauseAdvertisement(Long advertisementId, User admin);

    /**
     * Блокирует рекламу
     */
    void blockAdvertisement(Long advertisementId, String reason, String comment, User admin);

    /**
     * Отклоняет жалобы как необоснованные
     */
    void dismissReports(Long advertisementId, User admin);
}
