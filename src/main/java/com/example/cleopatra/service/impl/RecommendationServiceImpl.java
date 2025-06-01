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
    private static final int PAGE_SIZE = 12;
    private static final int TOP_RECOMMENDATIONS_SIZE = 6;


    private final UserRepository userRepository;
    private final RecommendationsMapper recommendationsMapper;

    /**
     * Топ рекомендации для главной страницы
     */
    @Override
    public List<UserRecommendationDto> getTopRecommendations(Long currentUserId) {
        try {
            log.debug("Получение топ рекомендаций для пользователя: {}", currentUserId);

            Pageable pageable = PageRequest.of(0, TOP_RECOMMENDATIONS_SIZE);
            List<User> topUsers = userRepository.findTopRecommendations(currentUserId, pageable);

            List<UserRecommendationDto> recommendations = topUsers.stream()
                    .map(user -> recommendationsMapper.mapToRecommendationDto(user, currentUserId))
                    .collect(Collectors.toList());

            log.debug("Найдено {} топ рекомендаций для пользователя {}",
                    recommendations.size(), currentUserId);

            return recommendations;

        } catch (Exception e) {
            log.error("Ошибка при получении топ рекомендаций для пользователя {}: {}",
                    currentUserId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Получить всех пользователей с пагинацией (без поиска)
     */
    @Override
    public UserRecommendationListDto getAllRecommendations(Long currentUserId, int page) {
        try {
            log.debug("Получение всех рекомендаций для пользователя: {}, страница: {}",
                    currentUserId, page);

            // Создаем Pageable с сортировкой по популярности
            Pageable pageable = PageRequest.of(page, PAGE_SIZE);

            // Используем поиск с пустым запросом (покажет всех пользователей)
            Slice<User> usersSlice = userRepository.findRecommendationsWithSearch(
                    currentUserId, "", pageable);

            // Маппим результаты
            List<UserRecommendationDto> recommendations = usersSlice.getContent()
                    .stream()
                    .map(user -> recommendationsMapper.mapToRecommendationDto(user, currentUserId))
                    .collect(Collectors.toList());

            log.debug("Найдено {} рекомендаций на странице {} для пользователя {}",
                    recommendations.size(), page, currentUserId);

            return UserRecommendationListDto.builder()
                    .userRecommendations(recommendations)
                    .currentPage(page)
                    .itemsPerPage(PAGE_SIZE)
                    .hasNext(usersSlice.hasNext())
                    .hasPrevious(usersSlice.hasPrevious())
                    .nextPage(usersSlice.hasNext() ? page + 1 : null)
                    .previousPage(usersSlice.hasPrevious() ? page - 1 : null)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при получении всех рекомендаций для пользователя {}, страница {}: {}",
                    currentUserId, page, e.getMessage(), e);
            return createEmptyRecommendations(page);
        }
    }

    /**
     * Поиск пользователей по запросу с пагинацией
     */
    @Override
    public UserRecommendationListDto searchRecommendations(
            Long currentUserId,
            String searchQuery,
            int page) {

        try {
            log.debug("Поиск рекомендаций для пользователя: {}, запрос: '{}', страница: {}",
                    currentUserId, searchQuery, page);

            // Создаем Pageable с сортировкой по популярности
            Pageable pageable = PageRequest.of(page, PAGE_SIZE);

            // Очищаем поисковый запрос
            String cleanQuery = (searchQuery != null) ? searchQuery.trim() : "";

            // Выполняем поиск
            Slice<User> usersSlice = userRepository.findRecommendationsWithSearch(
                    currentUserId, cleanQuery, pageable);

            // Маппим результаты
            List<UserRecommendationDto> recommendations = usersSlice.getContent()
                    .stream()
                    .map(user -> recommendationsMapper.mapToRecommendationDto(user, currentUserId))
                    .collect(Collectors.toList());

            log.debug("Найдено {} результатов поиска на странице {} для пользователя {}",
                    recommendations.size(), page, currentUserId);

            return UserRecommendationListDto.builder()
                    .userRecommendations(recommendations)
                    .currentPage(page)
                    .itemsPerPage(PAGE_SIZE)
                    .hasNext(usersSlice.hasNext())
                    .hasPrevious(usersSlice.hasPrevious())
                    .nextPage(usersSlice.hasNext() ? page + 1 : null)
                    .previousPage(usersSlice.hasPrevious() ? page - 1 : null)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при поиске рекомендаций для пользователя {}, запрос '{}', страница {}: {}",
                    currentUserId, searchQuery, page, e.getMessage(), e);
            return createEmptyRecommendations(page);
        }
    }

    /**
     * Создать пустой результат
     */
    private UserRecommendationListDto createEmptyRecommendations(int page) {
        return UserRecommendationListDto.builder()
                .userRecommendations(Collections.emptyList())
                .currentPage(page)
                .itemsPerPage(PAGE_SIZE)
                .hasNext(false)
                .hasPrevious(false)
                .nextPage(null)
                .previousPage(null)
                .build();
    }
}
