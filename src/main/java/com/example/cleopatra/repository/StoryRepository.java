package com.example.cleopatra.repository;

import com.example.cleopatra.model.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, Long> {

    Optional<Story> findByImageId(String imageId);

    @Query("SELECT s FROM Story s WHERE s.expiresAt > :now ORDER BY s.createdAt DESC")
    Page<Story> findAllActiveStories(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT s FROM Story s WHERE s.user.id = :userId AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s WHERE s.user.id IN (SELECT sub.subscribedTo.id FROM Subscription sub WHERE sub.subscriber.id = :userId) AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    Page<Story> findFollowingActiveStories(@Param("userId") Long userId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.user.id = :userId AND s.expiresAt > :now")
    Long countActiveStoriesByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Story s WHERE s.expiresAt <= :now")
    List<Story> findExpiredStories(@Param("now") LocalDateTime now);


    /**
     * Получить ID истекших историй с лимитом для batch обработки
     * @param now текущее время
     * @param pageable параметры пагинации (размер batch'а)
     * @return список ID истекших историй
     */
    @Query("SELECT s.id FROM Story s WHERE s.expiresAt <= :now ORDER BY s.expiresAt ASC")
    List<Long> findExpiredStoryIds(@Param("now") LocalDateTime now, Pageable pageable);
    /**
     * Найти активные истории пользователей из списка ID
     * @param userIds список ID пользователей (подписки)
     * @param now текущее время для проверки истечения
     * @param pageable параметры пагинации
     * @return страница активных историй
     */
    @Query("SELECT s FROM Story s WHERE s.user.id IN :userIds AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    Page<Story> findActiveStoriesByUserIds(@Param("userIds") List<Long> userIds, @Param("now") LocalDateTime now, Pageable pageable);
}