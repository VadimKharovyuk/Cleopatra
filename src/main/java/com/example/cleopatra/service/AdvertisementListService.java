package com.example.cleopatra.service;
import com.example.cleopatra.dto.AdvertisementDTO.AdvertisementList;
import com.example.cleopatra.enums.AdStatus;
import com.example.cleopatra.model.Advertisement;
import com.example.cleopatra.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AdvertisementListService {

    private final AdvertisementRepository advertisementRepository;

    // Все рекламы для админа
    public AdvertisementList getAllAdsForAdmin(Pageable pageable) {
        Page<Advertisement> page = advertisementRepository.findAllByOrderByCreatedAtDesc(pageable);
        return AdvertisementList.fromPage(page);
    }

    // Рекламы требующие модерации
    public AdvertisementList getAdsForModeration(Pageable pageable) {
        Page<Advertisement> page = advertisementRepository.findByStatusInOrderByCreatedAtDesc(
                Arrays.asList(AdStatus.PENDING), pageable);
        return AdvertisementList.fromPage(page);
    }

    // Рекламы с жалобами
    public AdvertisementList getAdsWithReports(Pageable pageable) {
        Page<Advertisement> page = advertisementRepository.findAdvertisementsWithReports(pageable);
        return AdvertisementList.fromPage(page);
    }

    // Рекламы пользователя
    public AdvertisementList getUserAds(Long userId, Pageable pageable) {
        Page<Advertisement> page = advertisementRepository.findByCreatedByIdOrderByCreatedAtDesc(userId, pageable);
        return AdvertisementList.fromPage(page);
    }

    // Поиск реклам
    public AdvertisementList searchAds(String search, Pageable pageable) {
        Page<Advertisement> page = advertisementRepository.searchAdvertisements(search, pageable);
        return AdvertisementList.fromPage(page);
    }
}