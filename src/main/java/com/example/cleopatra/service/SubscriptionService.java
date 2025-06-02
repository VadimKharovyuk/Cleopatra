package com.example.cleopatra.service;

import com.example.cleopatra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface SubscriptionService {


    ///подписка
    boolean subscribe(Long subscriberId, Long subscribedToId);
///отписка
    boolean unsubscribe(Long subscriberId, Long subscribedToId);


    /**
     * Переключить подписку (подписаться/отписаться)
     */
     boolean toggleSubscription(Long subscriberId, Long subscribedToId);


    /**
     * Проверяет, подписан ли пользователь
     */
     boolean isSubscribed(Long subscriberId, Long subscribedToId);

    /**
     * Получить подписчиков пользователя
     */
     Page<User> getSubscribers(Long userId, Pageable pageable);


    /**
     * Получить подписки пользователя
     */
     Page<User> getSubscriptions(Long userId, Pageable pageable);

    /**
     * Получить взаимные подписки (друзей)
     */
     List<User> getMutualSubscriptions(Long userId);


     Set<Long> getSubscribedToIdsFromList(Long subscriberId, List<Long> userIds);
}
