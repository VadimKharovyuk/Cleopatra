package com.example.cleopatra.repository;

import com.example.cleopatra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
   Optional<User>findByEmail(String email);

   boolean existsByEmail(String email);

    @Query("""
    SELECT u FROM User u 
    WHERE u.id != :currentUserId 
    AND u.firstName IS NOT NULL 
    AND u.lastName IS NOT NULL
    ORDER BY u.createdAt DESC
    """)
    List<User> findTopRecommendationsForUser(@Param("currentUserId") Long currentUserId, Pageable pageable);
    // Для страницы со всеми (с пагинацией через Slice)
    @Query("""
        SELECT u FROM User u 
        WHERE u.id != :currentUserId 
        AND u.firstName IS NOT NULL 
        AND u.lastName IS NOT NULL
        ORDER BY u.createdAt DESC
        """)
    Slice<User> findAllRecommendationsForUser(@Param("currentUserId") Long currentUserId, Pageable pageable);



    @Query("""
    SELECT u FROM User u 
    WHERE u.id != :currentUserId 
    AND u.firstName IS NOT NULL 
    AND u.lastName IS NOT NULL
    AND (:query = '' OR 
         LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%')))
    AND (:followers = 'all' OR
         (:followers = '0-10' AND u.followersCount BETWEEN 0 AND 10) OR
         (:followers = '10-100' AND u.followersCount BETWEEN 11 AND 100) OR
         (:followers = '100-1000' AND u.followersCount BETWEEN 101 AND 1000) OR
         (:followers = '1000+' AND u.followersCount > 1000))
    """)
    Slice<User> findRecommendationsWithFilters(@Param("currentUserId") Long currentUserId,
                                               @Param("query") String query,
                                               @Param("followers") String followers,
                                               @Param("status") String status,
                                               Pageable pageable);
}
