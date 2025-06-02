package com.example.cleopatra.maper;

import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionCard;
import com.example.cleopatra.dto.SubscriptionDto.UserSubscriptionListDto;
import com.example.cleopatra.model.Subscription;
import com.example.cleopatra.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Component
public class UserSubscriptionMapper {


    public UserSubscriptionCard mapToSubscriptionCard(User user, Subscription subscription) {
        if (user == null || subscription == null) {
            log.warn("Попытка маппинга с null параметрами: user={}, subscription={}", user, subscription);
            return null;
        }

        return UserSubscriptionCard.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .imageUrl(user.getImageUrl())
                .city(user.getCity())
                .subscribedAt(subscription.getCreatedAt())
                .isOnline(null) // TODO: реализовать позже
                .build();
    }


}
