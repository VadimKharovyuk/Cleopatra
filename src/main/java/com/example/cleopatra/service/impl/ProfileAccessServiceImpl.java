package com.example.cleopatra.service.impl;

import com.example.cleopatra.enums.ProfileAccessLevel;
import com.example.cleopatra.enums.Role;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.ProfileAccessService;
import com.example.cleopatra.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileAccessServiceImpl implements ProfileAccessService {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    @Override
    public boolean canViewProfile(Long viewerId, Long profileOwnerId) {
        log.info("🔒 canViewProfile: viewerId={}, profileOwnerId={}", viewerId, profileOwnerId);

        ProfileAccessLevel accessLevel = getProfileAccessLevel(profileOwnerId);
        log.info("🔒 Уровень доступа профиля: {}", accessLevel);

        boolean result = checkAccess(viewerId, profileOwnerId, accessLevel);
        log.info("🔒 Результат проверки доступа: {}", result);

        return result;
    }

    @Override
    public boolean canViewProfileSection(Long viewerId, Long profileOwnerId, String section) {
        // Базовая проверка доступа к профилю
        if (!canViewProfile(viewerId, profileOwnerId)) {
            return false;
        }

        // Здесь можно добавить дополнительную логику для разных разделов
        return switch (section.toLowerCase()) {
            case "photos" -> canViewProfile(viewerId, profileOwnerId);
            case "posts" -> canViewProfile(viewerId, profileOwnerId);
            case "personal_info" -> canViewProfile(viewerId, profileOwnerId);
            default -> canViewProfile(viewerId, profileOwnerId);
        };
    }

    @Override
    public String getAccessDeniedMessage(Long viewerId, Long profileOwnerId) {
        Optional<User> profileOwnerOpt = userRepository.findById(profileOwnerId);
        if (profileOwnerOpt.isEmpty()) {
            return "Пользователь не найден";
        }

        User profileOwner = profileOwnerOpt.get();

        if (profileOwner.getBlockedAt() == null) {
            return "Профиль недоступен";
        }

        ProfileAccessLevel accessLevel = profileOwner.getProfileAccessLevel();

        return switch (accessLevel) {
            case SUBSCRIBERS_ONLY -> "Подпишитесь на пользователя, чтобы видеть его профиль";
            case MUTUAL_SUBSCRIPTIONS -> "Этот профиль доступен только при взаимной подписке";
            case PRIVATE -> "Профиль закрыт для просмотра";
            default -> "Доступ к профилю ограничен";
        };
    }

    @Override
    public boolean canChangeAccessLevel(Long userId, Long profileOwnerId) {
        return userId != null &&
                (userId.equals(profileOwnerId) || isAdmin(userId));
    }

    @Override
    @Transactional
    public boolean updateProfileAccessLevel(Long userId, Long profileOwnerId, ProfileAccessLevel newAccessLevel) {
        // Проверяем права на изменение
        if (!canChangeAccessLevel(userId, profileOwnerId)) {
            log.warn("User {} tried to change access level for profile {}, but has no permission",
                    userId, profileOwnerId);
            return false;
        }

        // Находим пользователя
        Optional<User> userOpt = userRepository.findById(profileOwnerId);
        if (userOpt.isEmpty()) {
            log.error("Profile owner {} not found", profileOwnerId);
            return false;
        }

        User user = userOpt.get();
        ProfileAccessLevel oldLevel = user.getProfileAccessLevel();

        // Обновляем уровень доступа
        user.setProfileAccessLevel(newAccessLevel);
        userRepository.save(user);

        log.info("Profile access level changed for user {}: {} -> {} by user {}",
                profileOwnerId, oldLevel, newAccessLevel, userId);

        return true;
    }

    @Override
    public ProfileAccessLevel getProfileAccessLevel(Long profileOwnerId) {
        return userRepository.findById(profileOwnerId)
                .map(User::getProfileAccessLevel)
                .orElse(ProfileAccessLevel.PUBLIC); // По умолчанию публичный
    }

    @Override
    public boolean isSubscribedTo(Long subscriberId, Long subscribedToId) {
        return subscriptionService.isSubscribed(subscriberId, subscribedToId);
    }

    @Override
    public void logAccessAttempt(Long viewerId, Long profileOwnerId, boolean accessGranted) {
        if (!accessGranted) {
            log.info("Access denied: User {} tried to access profile of user {}",
                    viewerId != null ? viewerId : "anonymous",
                    profileOwnerId);
        }
    }

    /**
     * Проверяет, является ли пользователь администратором
     */
    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
    }

    /**
     * Проверяет, заблокирован ли пользователь
     */
    private boolean isBlocked(Long userId) {
        return userRepository.findById(userId)
                .map(User::isBlocked)  // Было User::getIsBlocked
                .orElse(false);
    }

    private boolean checkAccess(Long viewerId, Long profileOwnerId, ProfileAccessLevel accessLevel) {
        log.info("🔍 checkAccess: viewerId={}, profileOwnerId={}, accessLevel={}",
                viewerId, profileOwnerId, accessLevel);

        // Получаем владельца профиля
        Optional<User> profileOwnerOpt = userRepository.findById(profileOwnerId);
        if (profileOwnerOpt.isEmpty()) {
            log.warn("🔍 Пользователь {} не найден", profileOwnerId);
            return false;
        }

        User profileOwner = profileOwnerOpt.get();

        // Профиль всегда доступен самому владельцу
        if (viewerId != null && viewerId.equals(profileOwnerId)) {
            log.info("🔍 Доступ разрешен: собственный профиль");
            return true;
        }

        // Проверяем блокировку
        if (profileOwner.isBlocked()) {
            log.warn("🔍 Пользователь {} заблокирован системой", profileOwnerId);
            boolean isAdminAccess = viewerId != null && isAdmin(viewerId);
            log.info("🔍 Доступ админа: {}", isAdminAccess);
            return isAdminAccess;
        }

        // Admin имеет доступ ко всем профилям
        if (viewerId != null && isAdmin(viewerId)) {
            log.info("🔍 Доступ разрешен: администратор");
            return true;
        }

        // Заблокированный пользователь не может просматривать профили
        if (viewerId != null && isBlocked(viewerId)) {
            log.warn("🔍 Доступ запрещен: пользователь {} заблокирован", viewerId);
            return false;
        }

        // Проверяем уровень доступа
        boolean result = switch (accessLevel) {
            case PUBLIC -> {
                log.info("🔍 PUBLIC профиль - доступ разрешен");
                yield true;
            }

            case SUBSCRIBERS_ONLY -> {
                boolean isSubscribed = viewerId != null &&
                        subscriptionService.isSubscribed(viewerId, profileOwnerId);
                log.info("🔍 SUBSCRIBERS_ONLY: подписка {}={}", viewerId, isSubscribed);
                yield isSubscribed;
            }

            case MUTUAL_SUBSCRIPTIONS -> {
                boolean isSubscribed = viewerId != null &&
                        subscriptionService.isSubscribed(viewerId, profileOwnerId);
                boolean isMutual = viewerId != null &&
                        subscriptionService.isSubscribed(profileOwnerId, viewerId);
                log.info("🔍 MUTUAL_SUBSCRIPTIONS: подписка {}={}, взаимная {}",
                        viewerId, isSubscribed, isMutual);
                yield isSubscribed && isMutual;
            }

            case PRIVATE -> {
                log.info("🔍 PRIVATE профиль - доступ запрещен");
                yield false;
            }
        };

        log.info("🔍 Финальный результат checkAccess: {}", result);
        return result;
    }

    @Override
    @Transactional
    public boolean updatePhotosAccessLevel(Long userId, Long profileOwnerId, ProfileAccessLevel newAccessLevel) {
        if (!canChangeAccessLevel(userId, profileOwnerId)) {
            return false;
        }

        Optional<User> userOpt = userRepository.findById(profileOwnerId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setPhotosAccessLevel(newAccessLevel);
        userRepository.save(user);

        log.info("Photos access level changed for user {}: {} by user {}",
                profileOwnerId, newAccessLevel, userId);
        return true;
    }

    @Override
    @Transactional
    public boolean updatePostsAccessLevel(Long userId, Long profileOwnerId, ProfileAccessLevel newAccessLevel) {
        if (!canChangeAccessLevel(userId, profileOwnerId)) {
            return false;
        }

        Optional<User> userOpt = userRepository.findById(profileOwnerId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setPostsAccessLevel(newAccessLevel);
        userRepository.save(user);

        log.info("Posts access level changed for user {}: {} by user {}",
                profileOwnerId, newAccessLevel, userId);
        return true;
    }

    @Override
    public ProfileAccessLevel getPhotosAccessLevel(Long profileOwnerId) {
        return userRepository.findById(profileOwnerId)
                .map(User::getPhotosAccessLevel)
                .orElse(ProfileAccessLevel.PUBLIC);
    }

    @Override
    public ProfileAccessLevel getPostsAccessLevel(Long profileOwnerId) {
        return userRepository.findById(profileOwnerId)
                .map(User::getPostsAccessLevel)
                .orElse(ProfileAccessLevel.PUBLIC);
    }
}