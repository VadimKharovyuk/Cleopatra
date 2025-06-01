package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.user.UserRecommendationDto;
import com.example.cleopatra.dto.user.UserRecommendationListDto;
import com.example.cleopatra.maper.RecommendationsMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final UserRepository userRepository;
    private final RecommendationsMapper recommendationsMapper;


    private static final int DEFAULT_RECOMMENDATIONS_LIMIT = 5;
    private static final int PAGE_SIZE = 20; // размер страницы для полного списка

    @Override
    public List<UserRecommendationDto> getTopRecommendations(Long currentUserId) {
        try {
            log.info("🔍 Получение рекомендаций для пользователя: {}", currentUserId);

            List<User> users = userRepository.findTopRecommendationsForUser(currentUserId, PageRequest.of(0, DEFAULT_RECOMMENDATIONS_LIMIT));
            log.info("📊 Найдено пользователей в базе: {}", users.size());

            List<UserRecommendationDto> recommendations = users.stream()
                    .map(user -> {
                        log.debug("👤 Обрабатываем пользователя: {} {} (ID: {})",
                                user.getFirstName(), user.getLastName(), user.getId());
                        return recommendationsMapper.mapToRecommendationDto(user, currentUserId);
                    })
                    .collect(Collectors.toList());

            log.info("✅ Возвращаем рекомендаций: {}", recommendations.size());
            return recommendations;
        } catch (Exception e) {
            log.error("❌ Ошибка при получении топ рекомендаций для пользователя {}: {}", currentUserId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public UserRecommendationListDto getAllRecommendations(Long currentUserId, int page) {
        try {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
            Slice<User> usersSlice = userRepository.findAllRecommendationsForUser(currentUserId, pageable);

            List<UserRecommendationDto> recommendations = usersSlice.getContent()
                    .stream()
                    .map(user -> recommendationsMapper.mapToRecommendationDto(user, currentUserId))
                    .collect(Collectors.toList());

            return UserRecommendationListDto.builder()
                    .userRecommendations(recommendations)
                    .currentPage(page)
                    .itemsPerPage(PAGE_SIZE)
                    .totalPages(null) // Slice не предоставляет общее количество страниц
                    .totalItems(null) // Slice не предоставляет общее количество элементов
                    .hasNext(usersSlice.hasNext())
                    .hasPrevious(usersSlice.hasPrevious())
                    .nextPage(usersSlice.hasNext() ? page + 1 : null)
                    .previousPage(usersSlice.hasPrevious() ? page - 1 : null)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при получении всех рекомендаций для пользователя {}: {}", currentUserId, e.getMessage());
            return UserRecommendationListDto.builder()
                    .userRecommendations(Collections.emptyList())
                    .currentPage(page)
                    .itemsPerPage(PAGE_SIZE)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }
    }

    @Override
    public UserRecommendationListDto searchRecommendations(Long currentUserId, String query,
                                                           String sort, String followers,
                                                           String status, int page) {
        try {
            // Создаем Pageable с нужной сортировкой
            Sort sortOrder = createSortOrder(sort);
            Pageable pageable = PageRequest.of(page, PAGE_SIZE, sortOrder);

            // Выполняем поиск с фильтрами
            Slice<User> usersSlice = userRepository.findRecommendationsWithFilters(
                    currentUserId, query, followers, status, pageable);

            List<UserRecommendationDto> recommendations = usersSlice.getContent()
                    .stream()
                    .map(user -> recommendationsMapper.mapToRecommendationDto(user, currentUserId))
                    .collect(Collectors.toList());

            return UserRecommendationListDto.builder()
                    .userRecommendations(recommendations)
                    .currentPage(page)
                    .itemsPerPage(PAGE_SIZE)
                    .totalPages(null)
                    .totalItems(null)
                    .hasNext(usersSlice.hasNext())
                    .hasPrevious(usersSlice.hasPrevious())
                    .nextPage(usersSlice.hasNext() ? page + 1 : null)
                    .previousPage(usersSlice.hasPrevious() ? page - 1 : null)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при поиске рекомендаций: {}", e.getMessage());
            return UserRecommendationListDto.builder()
                    .userRecommendations(Collections.emptyList())
                    .currentPage(page)
                    .itemsPerPage(PAGE_SIZE)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }
    }

    private Sort createSortOrder(String sortType) {
        switch (sortType) {
            case "popular":
                return Sort.by("followersCount").descending();
            case "alphabetical":
                return Sort.by("firstName").ascending();
            case "active":
                return Sort.by("lastActiveAt").descending();
            case "newest":
            default:
                return Sort.by("createdAt").descending();
        }
    }
}
