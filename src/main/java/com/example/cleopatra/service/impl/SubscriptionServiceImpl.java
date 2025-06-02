package com.example.cleopatra.service.impl;

import com.example.cleopatra.model.Subscription;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.SubscriptionRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl  implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;


    @Override
    public boolean subscribe(Long subscriberId, Long subscribedToId) {
        try {
            // Проверяем, что пользователи разные
            if (subscriberId.equals(subscribedToId)) {
                log.warn("Пользователь {} пытается подписаться на себя", subscriberId);
                return false;
            }

            // Проверяем, не подписан ли уже
            if (subscriptionRepository.existsBySubscriberIdAndSubscribedToId(subscriberId, subscribedToId)) {
                log.info("Пользователь {} уже подписан на {}", subscriberId, subscribedToId);
                return false;
            }

            // Получаем пользователей
            User subscriber = userRepository.findById(subscriberId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found: " + subscriberId));
            User subscribedTo = userRepository.findById(subscribedToId)
                    .orElseThrow(() -> new RuntimeException("User to subscribe not found: " + subscribedToId));

            // Создаем подписку
            Subscription subscription = new Subscription(subscriber, subscribedTo);
            subscriptionRepository.save(subscription);

            // Обновляем счетчики
            updateSubscriptionCounts(subscriberId, subscribedToId);

            log.info("Пользователь {} подписался на {}", subscriberId, subscribedToId);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при подписке {} на {}: {}", subscriberId, subscribedToId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean unsubscribe(Long subscriberId, Long subscribedToId) {
        try {
            // Проверяем существование подписки
            if (!subscriptionRepository.existsBySubscriberIdAndSubscribedToId(subscriberId, subscribedToId)) {
                log.info("Подписка {} -> {} не найдена", subscriberId, subscribedToId);
                return false;
            }

            // Удаляем подписку
            subscriptionRepository.deleteBySubscriberIdAndSubscribedToId(subscriberId, subscribedToId);

            // Обновляем счетчики
            updateSubscriptionCounts(subscriberId, subscribedToId);

            log.info("Пользователь {} отписался от {}", subscriberId, subscribedToId);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отписке {} от {}: {}", subscriberId, subscribedToId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean toggleSubscription(Long subscriberId, Long subscribedToId) {
        if (isSubscribed(subscriberId, subscribedToId)) {
            return unsubscribe(subscriberId, subscribedToId);
        } else {
            return subscribe(subscriberId, subscribedToId);
        }
    }

    @Override
    public boolean isSubscribed(Long subscriberId, Long subscribedToId) {
        return subscriptionRepository.existsBySubscriberIdAndSubscribedToId(subscriberId, subscribedToId);
    }

    @Override
    public Page<User> getSubscribers(Long userId, Pageable pageable) {
        return subscriptionRepository.findRecentSubscribers(userId, pageable);
    }

    @Override
    public Page<User> getSubscriptions(Long userId, Pageable pageable) {
        return subscriptionRepository.findRecentSubscriptions(userId, pageable);
    }

    @Override
    public List<User> getMutualSubscriptions(Long userId) {
        return subscriptionRepository.findMutualSubscriptions(userId);
    }

    @Override
    public Set<Long> getSubscribedToIdsFromList(Long subscriberId, List<Long> userIds) {
        return Set.copyOf(subscriptionRepository.findSubscribedToIdsInList(subscriberId, userIds));
    }
    /**
     * Обновляет счетчики подписок и подписчиков
     */
    private void updateSubscriptionCounts(Long subscriberId, Long subscribedToId) {
        // Обновляем количество подписок у subscriber
        long subscriptionsCount = subscriptionRepository.countBySubscriberId(subscriberId);
        userRepository.updateFollowingCount(subscriberId, subscriptionsCount);

        // Обновляем количество подписчиков у subscribedTo
        long subscribersCount = subscriptionRepository.countBySubscribedToId(subscribedToId);
        userRepository.updateFollowersCount(subscribedToId, subscribersCount);
    }
}
