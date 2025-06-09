package com.example.cleopatra.repository;

import com.example.cleopatra.model.ConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, Long> {

    /**
     * Найти токен по значению
     */
    Optional<ConfirmationToken> findByToken(String token);

    /**
     * Найти все активные токены пользователя по email и типу
     */
    @Query("SELECT ct FROM ConfirmationToken ct WHERE ct.email = :email AND ct.tokenType = :tokenType AND ct.isUsed = false AND ct.expiresAt > :now")
    List<ConfirmationToken> findActiveTokensByEmailAndType(@Param("email") String email,
                                                           @Param("tokenType") ConfirmationToken.TokenType tokenType,
                                                           @Param("now") LocalDateTime now);

    /**
     * Найти все токены пользователя по email
     */
    List<ConfirmationToken> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Найти все токены пользователя по userId
     */
    List<ConfirmationToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Деактивировать все старые токены пользователя
     */
    @Modifying
    @Query("UPDATE ConfirmationToken ct SET ct.isUsed = true WHERE ct.email = :email AND ct.tokenType = :tokenType AND ct.isUsed = false")
    void deactivateAllTokensByEmailAndType(@Param("email") String email, @Param("tokenType") ConfirmationToken.TokenType tokenType);

    /**
     * Удалить все истекшие токены
     */
    @Modifying
    @Query("DELETE FROM ConfirmationToken ct WHERE ct.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Найти все истекшие токены
     */
    @Query("SELECT ct FROM ConfirmationToken ct WHERE ct.expiresAt < :now")
    List<ConfirmationToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Проверить существование активного токена
     */
    @Query("SELECT COUNT(ct) > 0 FROM ConfirmationToken ct WHERE ct.email = :email AND ct.tokenType = :tokenType AND ct.isUsed = false AND ct.expiresAt > :now")
    boolean existsActiveToken(@Param("email") String email,
                              @Param("tokenType") ConfirmationToken.TokenType tokenType,
                              @Param("now") LocalDateTime now);
}
