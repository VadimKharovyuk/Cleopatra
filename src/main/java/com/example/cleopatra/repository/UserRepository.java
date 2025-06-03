package com.example.cleopatra.repository;

import com.example.cleopatra.model.User;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        """)
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
        ORDER BY u.followersCount DESC, u.createdAt DESC
        """)
    List<User> findTopRecommendations(
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );


    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.followingCount = :count WHERE u.id = :userId")
    void updateFollowingCount(@Param("userId") Long userId, @Param("count") long count);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.followersCount = :count WHERE u.id = :userId")
    void updateFollowersCount(@Param("userId") Long userId, @Param("count") long count);

}
