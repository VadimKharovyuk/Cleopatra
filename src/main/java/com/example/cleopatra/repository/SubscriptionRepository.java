package com.example.cleopatra.repository;

import com.example.cleopatra.model.Subscription;
import com.example.cleopatra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Проверяет, подписан ли subscriber на subscribedTo
     */
    boolean existsBySubscriberIdAndSubscribedToId(Long subscriberId, Long subscribedToId);


    /**
     * Получает всех подписчиков пользователя
     */
    @Query("SELECT s FROM Subscription s WHERE s.subscribedTo.id = :userId ORDER BY s.createdAt DESC")
    Page<Subscription> findSubscribersByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Получает всех, на кого подписан пользователь
     */
    @Query("SELECT s FROM Subscription s WHERE s.subscriber.id = :userId ORDER BY s.createdAt DESC")
    Page<Subscription> findSubscriptionsByUserId(@Param("userId") Long userId, Pageable pageable);


    /**
     * Удаляет подписку
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Subscription s WHERE s.subscriber.id = :subscriberId AND s.subscribedTo.id = :subscribedToId")
    void deleteBySubscriberIdAndSubscribedToId(@Param("subscriberId") Long subscriberId,
                                               @Param("subscribedToId") Long subscribedToId);

    /**
     * Получает ID пользователей, на которых подписан данный пользователь
     */
    @Query("SELECT s.subscribedTo.id FROM Subscription s WHERE s.subscriber.id = :userId")
    List<Long> findSubscribedToIdsByUserId(@Param("userId") Long userId);

    /**
     * Получает ID подписчиков пользователя
     */
    @Query("SELECT s.subscriber.id FROM Subscription s WHERE s.subscribedTo.id = :userId")
    List<Long> findSubscriberIdsByUserId(@Param("userId") Long userId);

    /**
     * Взаимные подписки (друзья)
     */
    @Query("""

            SELECT s1.subscribedTo FROM Subscription s1 
        WHERE s1.subscriber.id = :userId 
        AND EXISTS (
            SELECT s2 FROM Subscription s2 
            WHERE s2.subscriber.id = s1.subscribedTo.id 
            AND s2.subscribedTo.id = :userId
        )
        ORDER BY s1.createdAt DESC
        """)
    List<User> findMutualSubscriptions(@Param("userId") Long userId);

    /**
     * Последние подписки пользователя
     */
    @Query("SELECT s.subscribedTo FROM Subscription s WHERE s.subscriber.id = :userId ORDER BY s.createdAt DESC")
    Page<User> findRecentSubscriptions(@Param("userId") Long userId, Pageable pageable);

    /**
     * Последние подписчики пользователя
     */
    @Query("SELECT s.subscriber FROM Subscription s WHERE s.subscribedTo.id = :userId ORDER BY s.createdAt DESC")
    Page<User> findRecentSubscribers(@Param("userId") Long userId, Pageable pageable);

    /**
     * Проверка множественных подписок (для batch операций)
     */
    @Query("SELECT s.subscribedTo.id FROM Subscription s WHERE s.subscriber.id = :subscriberId AND s.subscribedTo.id IN :userIds")
    List<Long> findSubscribedToIdsInList(@Param("subscriberId") Long subscriberId, @Param("userIds") List<Long> userIds);


    // Подписчики (кто подписался на пользователя)
    Slice<Subscription> findBySubscribedToId(Long subscribedToId, Pageable pageable);

    // Подписки (на кого подписан пользователь)
    Slice<Subscription> findBySubscriberId(Long subscriberId, Pageable pageable);

    /**
     * Подсчитать количество подписок пользователя (на кого он подписан)
     * @param subscriberId - ID пользователя
     * @return количество подписок
     */
    long countBySubscriberId(Long subscriberId);

    /**
     * Подсчитать количество подписчиков пользователя (кто на него подписан)
     * @param subscribedToId - ID пользователя
     * @return количество подписчиков
     */
    long countBySubscribedToId(Long subscribedToId);



    /**
     * Получить только ID пользователей, на которых подписан subscriberId
     */
    @Query("SELECT s.subscribedTo.id FROM Subscription s WHERE s.subscriber.id = :subscriberId")
    List<Long> findSubscribedToIdsBySubscriberId(@Param("subscriberId") Long subscriberId);
}