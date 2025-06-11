package com.example.cleopatra.repository;



import com.example.cleopatra.enums.StatType;
import com.example.cleopatra.model.AdStatistics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AdStatisticsRepository extends JpaRepository<AdStatistics, Long> {

    // Статистика по рекламе за период
    @Query("SELECT COUNT(s) FROM AdStatistics s WHERE s.advertisement.id = :adId AND s.type = :type AND s.createdAt BETWEEN :start AND :end")
    Long countByAdvertisementAndTypeAndPeriod(@Param("adId") Long adId,

                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    // Уникальные пользователи по рекламе
    @Query("SELECT COUNT(DISTINCT s.user) FROM AdStatistics s WHERE s.advertisement.id = :adId AND s.type = :type")
    Long countUniqueUsersByAdvertisementAndType(@Param("adId") Long adId, @Param("type") StatType type);

    // Статистика по часам для графиков
    @Query("SELECT HOUR(s.createdAt) as hour, COUNT(s) as count FROM AdStatistics s " +
            "WHERE s.advertisement.id = :adId AND s.type = :type AND DATE(s.createdAt) = DATE(:date) " +
            "GROUP BY HOUR(s.createdAt) ORDER BY hour")
    List<Object[]> getHourlyStats(@Param("adId") Long adId, @Param("type") StatType type, @Param("date") LocalDateTime date);

    // Топ стран по кликам
    @Query("SELECT s.country, COUNT(s) FROM AdStatistics s " +
            "WHERE s.advertisement.id = :adId AND s.type = 'CLICK' AND s.country IS NOT NULL " +
            "GROUP BY s.country ORDER BY COUNT(s) DESC")
    List<Object[]> getTopCountriesByClicks(@Param("adId") Long adId);
}
