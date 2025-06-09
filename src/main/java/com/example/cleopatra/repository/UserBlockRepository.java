package com.example.cleopatra.repository;

import com.example.cleopatra.model.User;
import com.example.cleopatra.model.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    /**
     * Найти блокировку по блокирующему и заблокированному пользователю
     */
    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);

    /**
     * Проверить существование блокировки
     */
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    /**
     * Найти всех заблокированных пользователей (отсортированы по дате блокировки)
     */
    List<UserBlock> findByBlockerOrderByBlockedAtDesc(User blocker);

    /**
     * Найти всех пользователей, которые заблокировали данного юзера
     */
    List<UserBlock> findByBlockedOrderByBlockedAtDesc(User blocked);

    /**
     * Подсчитать количество заблокированных пользователей
     */
    long countByBlocker(User blocker);

    /**
     * Подсчитать количество пользователей, которые заблокировали данного юзера
     */
    long countByBlocked(User blocked);

    /**
     * Найти все блокировки для пользователя (и заблокированные им, и заблокировавшие его)
     */
    @Query("SELECT ub FROM UserBlock ub WHERE ub.blocker = :user OR ub.blocked = :user ORDER BY ub.blockedAt DESC")
    List<UserBlock> findAllBlocksForUser(@Param("user") User user);

    /**
     * Проверить, заблокированы ли пользователи взаимно
     */
    @Query("SELECT COUNT(ub) > 0 FROM UserBlock ub WHERE " +
            "(ub.blocker = :user1 AND ub.blocked = :user2) OR " +
            "(ub.blocker = :user2 AND ub.blocked = :user1)")
    boolean areUsersBlockedMutually(@Param("user1") User user1, @Param("user2") User user2);

    /**
     * Получить ID всех пользователей, заблокированных данным пользователем
     */
    @Query("SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker = :blocker")
    Set<Long> findBlockedUserIds(@Param("blocker") User blocker);

    /**
     * Получить ID всех пользователей, которые заблокировали данного пользователя
     */
    @Query("SELECT ub.blocker.id FROM UserBlock ub WHERE ub.blocked = :blocked")
    Set<Long> findBlockerUserIds(@Param("blocked") User blocked);

    /**
     * Найти все блокировки между списком пользователей (для оптимизации запросов)
     */
    @Query("SELECT ub FROM UserBlock ub WHERE ub.blocker.id IN :userIds AND ub.blocked.id IN :userIds")
    List<UserBlock> findBlocksBetweenUsers(@Param("userIds") List<Long> userIds);

    /**
     * Проверить, заблокирован ли пользователь по ID
     */
    @Query("SELECT COUNT(ub) > 0 FROM UserBlock ub WHERE ub.blocker.id = :blockerId AND ub.blocked.id = :blockedId")
    boolean existsByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    /**
     * Удалить все блокировки пользователя (при удалении аккаунта)
     */
    @Query("DELETE FROM UserBlock ub WHERE ub.blocker = :user OR ub.blocked = :user")
    void deleteAllUserBlocks(@Param("user") User user);

    /**
     * Найти заблокированных пользователей с пагинацией
     */
    Page<UserBlock> findByBlockerOrderByBlockedAtDesc(User blocker, Pageable pageable);

    /**
     * Найти блокировки с пагинацией
     */
    @Query("SELECT ub FROM UserBlock ub WHERE ub.blocker = :blocker ORDER BY ub.blockedAt DESC")
    Page<UserBlock> findByBlockerWithPagination(@Param("blocker") User blocker,
                                                org.springframework.data.domain.Pageable pageable);

    /**
     * Найти заблокированных пользователей по частичному совпадению имени/email
     */
    @Query("SELECT ub FROM UserBlock ub WHERE ub.blocker = :blocker AND " +
            "(LOWER(ub.blocked.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(ub.blocked.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(ub.blocked.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY ub.blockedAt DESC")
    List<UserBlock> searchBlockedUsers(@Param("blocker") User blocker, @Param("searchTerm") String searchTerm);
}