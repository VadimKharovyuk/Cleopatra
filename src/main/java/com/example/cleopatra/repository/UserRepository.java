package com.example.cleopatra.repository;

import com.example.cleopatra.enums.Role;
import com.example.cleopatra.model.User;
import org.hibernate.query.SelectionQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Универсальный поиск рекомендаций с сортировкой по популярности
     * Работает как для поиска, так и для просмотра всех пользователей
     */
    @Query("""

            SELECT u FROM User u
            WHERE u.id != :currentUserId
            AND u.firstName IS NOT NULL
            AND u.lastName IS NOT NULL
            AND (:searchQuery = '' OR 
             LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
             LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
             LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchQuery, '%')))
            ORDER BY u.followersCount DESC, u.createdAt DESC
        """
    )
    Slice<User> findRecommendationsWithSearch(
            @Param("currentUserId") Long currentUserId,
            @Param("searchQuery") String searchQuery,
            Pageable pageable
    );

    /**
     * Топ рекомендации для главной страницы (ограниченное количество)
     */
    @Query("""

            SELECT u FROM User u 
    WHERE u.id != :currentUserId 
    AND u.firstName IS NOT NULL 
    AND u.lastName IS NOT NULL
    ORDER BY 
        CASE 
            WHEN u.id = 1 THEN 6                   
            WHEN u.followersCount >= 10000 THEN 5
            WHEN u.followersCount >= 1000 THEN 4
            WHEN u.followersCount >= 100 THEN 3
            WHEN u.followersCount >= 10 THEN 2
            ELSE 1
        END DESC,
        u.followersCount DESC,
        u.createdAt DESC
    """)
    List<User> findTopRecommendations(
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );



    /**
     * Найти пользователя по email с загруженным онлайн статусом
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.onlineStatus WHERE u.email = :email")
    Optional<User> findByEmailWithOnlineStatus(@Param("email") String email);

    /**
     * Найти пользователя по ID с загруженным онлайн статусом
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.onlineStatus WHERE u.id = :id")
    Optional<User> findByIdWithOnlineStatus(@Param("id") Long id);

    /**
     * Найти нескольких пользователей по ID с их онлайн статусами
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.onlineStatus WHERE u.id IN :userIds")
    List<User> findByIdInWithOnlineStatus(@Param("userIds") List<Long> userIds);

    /**
     * Рекомендации с онлайн статусами (опти
     * ная версия)
     */
    @Query
            ("""
        SELECT u FROM
            User u 
        LEFT JOIN FETCH u
            .onlineStatus
        WHERE
            u.id != :currentUserId 
        AND u.firstName IS NOT NULL 
        AND u.lastName IS NOT NULL
        AND (:searchQuery = '' OR 
             LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
             LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
             LOWER(CONCAT(u.
            firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%',
            :
    searchQuery, '%')))
        ORDER BY u.followersCount DESC, u.createdAt DESC
        """)

    Slice<User> findRecommendationsWithSearchAndOnlineStatus(
            @Param("currentUserId") Long currentUserId,
            @Param("searchQuery") String searchQuery,
            Pageable pageable
    );


    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);


    List<User> findByIsOnlineTrue();

    @Query("SELECT u.isOnline FROM User u WHERE u.id = :userId")
    Optional<Boolean> findOnlineStatusById(@Param("userId") Long userId);

    // ✅ НОВЫЙ МЕТОД: Очистка неактивных пользователей
    @Modifying
    @Query("UPDATE User u SET u.isOnline = false, u.lastSeen = u.lastActivity " +
            "WHERE u.isOnline = true AND u.lastActivity < :threshold")
    int setOfflineForInactiveUsers(@Param("threshold") LocalDateTime threshold);



    // Батчевое обновление
    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET " +
            "is_online = false, " +
            "last_seen = NOW() " +
            "WHERE id IN (" +
            "  SELECT id FROM users " +
            "  WHERE is_online = true " +
            "  AND last_activity < :threshold " +
            "  ORDER BY last_activity ASC " +
            "  LIMIT :batchSize" +
            ")",
            nativeQuery = true)
    int markUsersOfflineByLastActivityBatch(@Param("threshold") LocalDateTime threshold,
                                            @Param("batchSize") int batchSize);

    // Для статистики
    @Query(value = "SELECT COUNT(*) FROM users WHERE is_online = true", nativeQuery = true)
    long countOnlineUsers();

    @Query(value = "SELECT COUNT(*) FROM users WHERE is_online = true AND last_activity < :threshold", nativeQuery = true)
    long countInactiveUsers(@Param("threshold") LocalDateTime threshold);


    // Поиск по имени или фамилии (частичное совпадение)
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);
    
    Page<User> findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String email, String firstName, String lastName, Pageable pageable);


    Page<User> findByRole(Role role, Pageable pageable);




    /**
     * Находит пользователей по упоминаниям (имя, фамилия или полное имя)
     */
    @Query("""
        SELECT DISTINCT u FROM User u 
        WHERE LOWER(u.firstName) IN :mentions 
           OR LOWER(u.lastName) IN :mentions 
           OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) IN :mentions
           OR LOWER(CONCAT(u.lastName, ' ', u.firstName)) IN :mentions
        """)
    List<User> findUsersByMentions(@Param("mentions") Set<String> mentions);

    /**
     * Поиск пользователей для автодополнения упоминаний
     */
    @Query("""
    SELECT u FROM User u 
    WHERE LOWER(u.firstName) LIKE LOWER(CONCAT(:query, '%'))
       OR LOWER(u.lastName) LIKE LOWER(CONCAT(:query, '%'))
       OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY u.firstName, u.lastName
    LIMIT 10
    """)
    List<User> searchUsersForMentions(@Param("query") String query);
}

