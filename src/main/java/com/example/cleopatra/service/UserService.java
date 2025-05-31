package com.example.cleopatra.service;

import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserResponse;
import org.springframework.web.multipart.MultipartFile;


public interface UserService {

    UserResponse createUser(RegisterDto  registerDto);

    // Только загрузка/обновление аватара
 UserResponse uploadAvatar(Long userId, MultipartFile file) ;

//    // Только обновление текстовых полей профиля
  UserResponse updateProfile(Long userId, UpdateProfileDto profileDto);


    UserResponse getUserById(Long userId);
    /**
     * Проверяет существование пользователя и выбрасывает исключение если не найден
     *
     * КОГДА ИСПОЛЬЗОВАТЬ:
     * - Перед выполнением ОБЯЗАТЕЛЬНЫХ операций
     * - В методах, где пользователь ДОЛЖЕН существовать
     * - Для защиты бизнес-логики от несуществующих пользователей
     * - В начале методов для "fail fast" принципа
     *
     * ПРИМЕРЫ:
     * - sendMessage(senderId, receiverId) - проверить обоих пользователей
     * - followUser(userId, targetUserId) - проверить существование
     * - createPost(authorId, content) - проверить автора
     * - updateUserSettings(userId, settings) - проверить пользователя
     * - blockUser(userId, targetUserId) - проверить обоих
     *
     * @param userId ID пользователя для проверки
     * @throws RuntimeException если пользователь не найден
     */
    void validateUserExists(Long userId);

    /**
     * Проверяет существование пользователя без выбрасывания исключения
     *
     * КОГДА ИСПОЛЬЗОВАТЬ:
     * - Для УСЛОВНОЙ логики (if-else)
     * - Когда нужно разное поведение в зависимости от существования
     * - Для опциональных операций
     * - В валидаторах форм
     *
     * ПРИМЕРЫ:
     * - if (userExists(id)) showProfile() else showRegistration()
     * - Проверка доступности username при регистрации
     * - Условная отправка уведомлений
     * - Миграция данных (проверить перед созданием)
     * - Кэширование (проверить перед загрузкой)
     *
     * @param userId ID пользователя для проверки
     * @return true если пользователь существует, false если нет
     */
    boolean userExists(Long userId);


    void deleteAvatar(Long userId);
}
