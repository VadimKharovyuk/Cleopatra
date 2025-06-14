package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.BirthdayUser.BirthdayPageResponse;
import com.example.cleopatra.dto.BirthdayUser.BirthdayUserCardDto;
import com.example.cleopatra.maper.BirthdayUserMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.BirthdayService;
import com.example.cleopatra.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BirthdayServiceImpl implements BirthdayService {



    private final SubscriptionService subscriptionService ;
    private final BirthdayUserMapper birthdayUserMapper ;
    private final UserRepository userRepository;

    @Override
    public BirthdayPageResponse getSubscriptionsBirthdays(Long userId, Pageable pageable) {

        // Получаем ID всех подписок пользователя
        List<Long> subscriptionIds = subscriptionService.getSubscriptionIds(userId);

        if (subscriptionIds.isEmpty()) {
            return createEmptyResponse(pageable);
        }

        // Получаем пользователей-подписок с днями рождения (не null)
        Page<User> usersPage = userRepository.findUsersWithBirthdaysByIds(
                subscriptionIds,
                pageable
        );

        // Фильтруем по приватности (показывают ли день рождения)
        List<User> visibleBirthdayUsers = usersPage.getContent().stream()
                .filter(user -> user.getShowBirthday() != null && user.getShowBirthday())
                .collect(Collectors.toList());

        // Маппим в DTO
        List<BirthdayUserCardDto> birthdayCards = birthdayUserMapper.toDtoList(visibleBirthdayUsers);

        // Создаем ответ
        BirthdayPageResponse response = new BirthdayPageResponse();
        response.setUsers(birthdayCards);
        response.setHasNext(usersPage.hasNext());
        response.setCurrentPage(usersPage.getNumber());
        response.setSize(usersPage.getSize());
        response.setIsEmpty(birthdayCards.isEmpty());
        response.setNumberOfElements(birthdayCards.size());
        response.setPeriod("all");
        response.setTotalBirthdays(birthdayCards.size());

        return response;
    }

    private BirthdayPageResponse createEmptyResponse(Pageable pageable) {
        BirthdayPageResponse response = new BirthdayPageResponse();
        response.setUsers(Collections.emptyList());
        response.setHasNext(false);
        response.setCurrentPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        response.setIsEmpty(true);
        response.setNumberOfElements(0);
        response.setPeriod("all");
        response.setTotalBirthdays(0);
        return response;
    }
}
