package com.example.cleopatra.service;

import com.example.cleopatra.model.User;

public interface AdvertisementModerationService {

    /**
     * Одобряет рекламу
     */
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