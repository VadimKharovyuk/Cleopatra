package com.example.cleopatra.service.impl;

import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.model.Advertisement;
import com.example.cleopatra.model.User;

import com.example.cleopatra.repository.AdvertisementRepository;
import com.example.cleopatra.service.AdvertisementModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdvertisementModerationServiceImpl implements AdvertisementModerationService {

    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementRepository adReportRepository; // Нужно создать если нет

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
        if (adReportRepository != null) {
            // adReportRepository.dismissReportsForAdvertisement(advertisementId, admin);
        }

        log.info("Жалобы для рекламы {} отклонены как необоснованные", advertisementId);
    }
}