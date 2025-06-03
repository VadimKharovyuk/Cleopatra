package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionCard;
import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionDto;
import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionListDto;
import com.example.cleopatra.maper.UserSubscriptionMapper;
import com.example.cleopatra.model.Subscription;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.SubscriptionRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl  implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
 private final UserSubscriptionMapper subscriptionMapper ;

    @Override
    @Transactional  // ← Обязательно добавь это!
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
    @Transactional
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
    public UserSubscriptionListDto getSubscribers(Long userId, Pageable pageable) {
        try {
            log.debug("Получение подписчиков для пользователя: {}, страница: {}",
                    userId, pageable.getPageNumber());

            Slice<Subscription> subscriptionsSlice = subscriptionRepository.findBySubscribedToId(userId, pageable);


            List<UserSubscriptionCard> cards = subscriptionsSlice.getContent()
                    .stream()
                    .map(subscription -> subscriptionMapper.mapToSubscriptionCard(
                            subscription.getSubscriber(), // Тот кто подписался
                            subscription
                    ))
                    .collect(Collectors.toList());

            log.debug("Найдено {} подписчиков на странице {} для пользователя {}",
                    cards.size(), pageable.getPageNumber(), userId);

            // Строим итоговый DTO с пагинацией через Slice
            return UserSubscriptionListDto.builder()
                    .subscriptions(cards)
                    .currentPage(pageable.getPageNumber())
                    .itemsPerPage(pageable.getPageSize())

                    .totalPages(null)
                    .totalItems(null)

                    // Slice данные
                    .hasNext(subscriptionsSlice.hasNext())
                    .hasPrevious(subscriptionsSlice.hasPrevious())
                    .nextPage(subscriptionsSlice.hasNext() ? pageable.getPageNumber() + 1 : null)
                    .previousPage(subscriptionsSlice.hasPrevious() ? pageable.getPageNumber() - 1 : null)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при получении подписчиков для пользователя {}, страница {}: {}",
                    userId, pageable.getPageNumber(), e.getMessage(), e);
            return createEmptySubscriptionsSlice(pageable.getPageNumber(), pageable.getPageSize());
        }
    }

    /**
     * Создает пустой DTO для Slice в случае ошибки
     */
    private UserSubscriptionListDto createEmptySubscriptionsSlice(int page, int size) {
        return UserSubscriptionListDto.builder()
                .subscriptions(Collections.emptyList())
                .currentPage(page)
                .itemsPerPage(size)
                .totalPages(null)     // Slice не знает общее количество
                .totalItems(null)     // Slice не знает общее количество
                .hasNext(false)
                .hasPrevious(page > 0)
                .nextPage(null)
                .previousPage(page > 0 ? page - 1 : null)
                .build();
    }



    @Override
    public UserSubscriptionListDto getSubscriptions(Long userId, Pageable pageable) {
        try {
            log.debug("Получение подписок для пользователя: {}, страница: {}",
                    userId, pageable.getPageNumber());

            // Используем Slice для подписок пользователя (на кого он подписан)
            Slice<Subscription> subscriptionsSlice = subscriptionRepository.findBySubscriberId(userId, pageable);

            // Маппим содержимое страницы в карточки
            List<UserSubscriptionCard> cards = subscriptionsSlice.getContent()
                    .stream()
                    .map(subscription -> subscriptionMapper.mapToSubscriptionCard(
                            subscription.getSubscribedTo(), // На кого подписан
                            subscription
                    ))
                    .collect(Collectors.toList());

            log.debug("Найдено {} подписок на странице {} для пользователя {}",
                    cards.size(), pageable.getPageNumber(), userId);

            // Строим итоговый DTO с пагинацией через Slice
            return UserSubscriptionListDto.builder()
                    .subscriptions(cards)
                    .currentPage(pageable.getPageNumber())
                    .itemsPerPage(pageable.getPageSize())

                    // Для Slice НЕТ totalPages и totalItems (избегаем COUNT запрос)
                    .totalPages(null)
                    .totalItems(null)

                    // Slice данные
                    .hasNext(subscriptionsSlice.hasNext())
                    .hasPrevious(subscriptionsSlice.hasPrevious())
                    .nextPage(subscriptionsSlice.hasNext() ? pageable.getPageNumber() + 1 : null)
                    .previousPage(subscriptionsSlice.hasPrevious() ? pageable.getPageNumber() - 1 : null)
                    .build();

        } catch (Exception e) {
            log.error("Ошибка при получении подписок для пользователя {}, страница {}: {}",
                    userId, pageable.getPageNumber(), e.getMessage(), e);
            return createEmptySubscriptionsSlice(pageable.getPageNumber(), pageable.getPageSize());
        }
    }


    /**
     * Обновляет счетчики подписок и подписчиков
     */
    /**
     * Обновляет счетчики подписок и подписчиков
     */
    private void updateSubscriptionCounts(Long subscriberId, Long subscribedToId) {
        try {
            // Обновляем количество подписок у subscriber
            long subscriptionsCount = subscriptionRepository.countBySubscriberId(subscriberId);
            User subscriber = userRepository.findById(subscriberId).orElse(null);
            if (subscriber != null) {
                subscriber.setFollowingCount(subscriptionsCount);
                userRepository.save(subscriber);
            }

            // Обновляем количество подписчиков у subscribedTo
            long subscribersCount = subscriptionRepository.countBySubscribedToId(subscribedToId);
            User subscribedTo = userRepository.findById(subscribedToId).orElse(null);
            if (subscribedTo != null) {
                subscribedTo.setFollowersCount(subscribersCount);
                userRepository.save(subscribedTo);
            }

            log.debug("Счетчики обновлены: subscriber {} -> {}, subscribedTo {} -> {}",
                    subscriberId, subscriptionsCount, subscribedToId, subscribersCount);

        } catch (Exception e) {
            log.error("Ошибка при обновлении счетчиков: {}", e.getMessage(), e);
        }
    }
}
