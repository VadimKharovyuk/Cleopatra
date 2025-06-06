package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.VisitDto.VisitDtoCard;
import com.example.cleopatra.dto.VisitDto.VisitListDto;
import com.example.cleopatra.dto.VisitDto.VisitStatsDto;
import com.example.cleopatra.maper.VisitMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.model.Visit;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.repository.VisitRepository;
import com.example.cleopatra.service.NotificationService;
import com.example.cleopatra.service.VisitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j


public class VisitServiceImpl implements VisitService {

    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final VisitMapper visitMapper;
    private final NotificationService notificationService;

    @Override
    public void recordVisit(Long visitedUserId, Long visitorId, String ipAddress, String userAgent) {
        // Проверяем, что пользователь не посещает сам себя
        if (Objects.equals(visitedUserId, visitorId)) {
            return;
        }

        try {
            // Проверяем, был ли визит в последние N минут (избегаем дублирования)
            LocalDateTime recentVisitThreshold = LocalDateTime.now().minusMinutes(5);
            boolean recentVisitExists = visitRepository.existsByVisitedUserIdAndVisitorIdAndVisitedAtAfter(
                    visitedUserId, visitorId, recentVisitThreshold);

            if (!recentVisitExists) {
                User visitedUser = userRepository.findById(visitedUserId).orElse(null);
                User visitor = userRepository.findById(visitorId).orElse(null);

                if (visitedUser != null && visitor != null) {
                    Visit visit = Visit.builder()
                            .visitedUser(visitedUser)
                            .visitor(visitor)
                            .visitedAt(LocalDateTime.now())
                            .ipAddress(ipAddress)
                            .userAgent(userAgent)
                            .build();

                    visitRepository.save(visit);

                    // Обновляем счетчик
                    Long currentVisits = visitedUser.getTotalVisits();
                    visitedUser.setTotalVisits((currentVisits != null ? currentVisits : 0L) + 1);

                    notificationService.createProfileVisitNotification(visitedUserId, visitorId);


                    userRepository.save(visitedUser);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при записи визита: {}", e.getMessage());
        }
    }

    @Override
    public VisitListDto getRecentVisitors(Long userId, Pageable pageable) {
        try {
            Page<Visit> visitPage = visitRepository.findByVisitedUserIdOrderByVisitedAtDesc(userId, pageable);
            return visitMapper.toVisitListDto(visitPage);
        } catch (Exception e) {
            log.error("Ошибка при получении посетителей для пользователя {}: {}", userId, e.getMessage());
            return visitMapper.createEmptyVisitListDto(pageable.getPageNumber(), pageable.getPageSize());
        }
    }

    @Override
    public List<VisitDtoCard> getUniqueVisitors(Long userId, LocalDateTime from, LocalDateTime to) {
        try {
            List<Visit> visits = visitRepository.findUniqueVisitsByUserIdAndDateRange(userId, from, to);
            return visitMapper.toVisitDtoCardList(visits);
        } catch (Exception e) {
            log.error("Ошибка при получении уникальных посетителей: {}", e.getMessage());
            return new ArrayList<>();
        }
    }


    @Override
    public VisitStatsDto getVisitStats(Long userId) {
        try {
            if (userId == null || userId <= 0) {
                log.warn("Некорректный userId для получения статистики: {}", userId);
                return createEmptyVisitStats();
            }

            LocalDateTime now = LocalDateTime.now();

            // Начало сегодняшнего дня
            LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();

            // Начало недели (понедельник)
            LocalDateTime startOfWeek = now.minusWeeks(1);

            // Начало месяца
            LocalDateTime startOfMonth = now.minusMonths(1);

            // Получаем статистику
            Long totalVisits = visitRepository.countByVisitedUserId(userId);
            Long todayVisits = visitRepository.countByVisitedUserIdAndVisitedAtAfter(userId, startOfToday);
            Long weekVisits = visitRepository.countByVisitedUserIdAndVisitedAtAfter(userId, startOfWeek);
            Long monthVisits = visitRepository.countByVisitedUserIdAndVisitedAtAfter(userId, startOfMonth);
            Long uniqueVisitorsCount = visitRepository.countUniqueVisitorsByUserId(userId);

            return VisitStatsDto.builder()
                    .totalVisits(totalVisits != null ? totalVisits : 0L)
                    .todayVisits(todayVisits != null ? todayVisits : 0L)
                    .weekVisits(weekVisits != null ? weekVisits : 0L)
                    .monthVisits(monthVisits != null ? monthVisits : 0L)
                    .uniqueVisitorsCount(uniqueVisitorsCount != null ? uniqueVisitorsCount : 0L)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при получении статистики визитов для пользователя {}: {}", userId, e.getMessage());
            return createEmptyVisitStats();
        }
    }


    private VisitStatsDto createEmptyVisitStats() {
        return VisitStatsDto.builder()
                .totalVisits(0L)
                .todayVisits(0L)
                .weekVisits(0L)
                .monthVisits(0L)
                .uniqueVisitorsCount(0L)
                .build();
    }


}
