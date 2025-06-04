package com.example.cleopatra.dto.ChatMessage;// UserBriefDto - краткая информация о пользователе для чата

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBriefDto {

    // Основная информация
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;

    // Аватар пользователя
    private String imageUrl;

    // Статус онлайн
    private Boolean isOnline;
    private String lastSeenText; // "в сети", "был час назад", "недавно"

    // Время последнего посещения (для точного отображения)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSeen;

    // Дополнительная информация для чата
    private String username; // @username если есть
    private String bio; // Краткое описание

    // Статусы для чата
    private Boolean isTyping; // Печатает ли сейчас
    private Boolean isBlocked; // Заблокирован ли этот пользователь
    private Boolean hasBlockedMe; // Заблокировал ли меня этот пользователь

    // Информация о дружбе/подписке
    private Boolean isFriend; // Друзья ли мы
    private Boolean isFollowing; // Подписан ли я на него
    private Boolean isFollower; // Подписан ли он на меня

    // Счетчики для профиля
    private Long postsCount;
    private Long followersCount;
    private Long followingCount;

    // Приватность
    private Boolean isPrivateProfile; // Закрытый ли профиль

    // Верификация
    private Boolean isVerified; // Верифицированный аккаунт

    // Роль пользователя (если нужно показывать в чате)
    private String role; // USER, ADMIN, MODERATOR

    // Для группировки в списках
    private String displayLetter; // Первая буква имени для группировки

    // Дополнительные методы для удобства

    /**
     * Получить отображаемое имя (firstName lastName или username)
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        if (firstName != null) {
            return firstName;
        }
        if (username != null) {
            return "@" + username;
        }
        return "Пользователь";
    }

    /**
     * Получить инициалы для аватара-заглушки
     */
    public String getInitials() {
        if (firstName != null && lastName != null &&
                !firstName.isEmpty() && !lastName.isEmpty()) {
            return firstName.substring(0, 1).toUpperCase() +
                    lastName.substring(0, 1).toUpperCase();
        }
        if (firstName != null && !firstName.isEmpty()) {
            return firstName.substring(0, Math.min(2, firstName.length())).toUpperCase();
        }
        if (username != null && !username.isEmpty()) {
            return username.substring(0, Math.min(2, username.length())).toUpperCase();
        }
        return "??";
    }

    /**
     * Проверить, доступен ли профиль для просмотра
     */
    public boolean isProfileAccessible() {
        // Если профиль не приватный - доступен всем
        if (isPrivateProfile == null || !isPrivateProfile) {
            return true;
        }
        // Если приватный, но мы друзья - доступен
        return isFriend != null && isFriend;
    }

    /**
     * Получить CSS класс для статуса онлайн
     */
    public String getOnlineStatusClass() {
        if (isOnline != null && isOnline) {
            return "user-online";
        }
        if (lastSeen != null && lastSeen.isAfter(
                java.time.LocalDateTime.now().minusMinutes(5))) {
            return "user-recently-online";
        }
        return "user-offline";
    }

    /**
     * Можно ли отправить сообщение этому пользователю
     */
    public boolean canSendMessage() {
        // Нельзя если заблокирован
        if (isBlocked != null && isBlocked) {
            return false;
        }
        // Нельзя если он заблокировал меня
        if (hasBlockedMe != null && hasBlockedMe) {
            return false;
        }
        // Можно если друзья
        if (isFriend != null && isFriend) {
            return true;
        }
        // Можно если профиль не приватный
        return !isPrivateProfile();
    }

    private boolean isPrivateProfile() {
        return isPrivateProfile != null && isPrivateProfile;
    }
}
