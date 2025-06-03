package com.example.cleopatra.service;

import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionDto;
import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionListDto;
import com.example.cleopatra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface SubscriptionService {


    /// подписка
    boolean subscribe(Long subscriberId, Long subscribedToId);

    /// отписка
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
    UserSubscriptionListDto getSubscribers(Long userId, Pageable pageable);


    /**
     * Получить подписки пользователя
     */
    UserSubscriptionListDto getSubscriptions(Long userId, Pageable pageable);

    /**
     * Получить ID всех пользователей, на которых подписан данный пользователь
     */
    List<Long> getSubscriptionIds(Long subscriberId);

    /**
     * Проверить подписан ли пользователь на кого-либо
     */
    boolean hasAnySubscriptions(Long subscriberId);

}
