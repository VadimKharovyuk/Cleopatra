package com.example.cleopatra.service.impl;
import com.example.cleopatra.ExistsException.InvalidPasswordException;
import com.example.cleopatra.ExistsException.PasswordMismatchException;
import com.example.cleopatra.ExistsException.SamePasswordException;
import com.example.cleopatra.ExistsException.UserAlreadyExistsException;
import com.example.cleopatra.dto.ChatMessage.UserBriefDto;
import com.example.cleopatra.dto.user.ChangePasswordDto;
import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.ProfileAccessLevel;
import com.example.cleopatra.maper.UserMapper;
import com.example.cleopatra.model.User;
import com.example.cleopatra.model.UserOnlineStatus;
import com.example.cleopatra.repository.*;
import com.example.cleopatra.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ImageValidator imageValidator;
    private final StorageService storageService;
    private final SubscriptionRepository subscriptionRepository;
    private final PostRepository postRepository;
    private final UserOnlineStatusRepository onlineStatusRepository;
    private final SubscriptionService subscriptionService;
    private final SystemBlockRepository systemBlockRepository;
    private final ProfileAccessService profileAccessService;

    @Override
    public UserResponse createUser(RegisterDto registerDto) {
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new UserAlreadyExistsException("Email уже используется");
        }
        User user = userMapper.toEntity(registerDto);
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
//    @CacheEvict(value = "users", key = "#userId")
    public UserResponse uploadAvatar(Long userId, MultipartFile file) {
        log.info("Начинаем загрузку аватара для пользователя с ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

            ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(file);

            if (!processedImage.getContentType().equals(file.getContentType())) {
                log.info("Файл {} был конвертирован из {} в {}",
                        file.getOriginalFilename(),
                        file.getContentType(),
                        processedImage.getContentType());
            }

            if (user.getImgId() != null && !user.getImgId().isEmpty()) {
                log.debug("Удаляем старый аватар с ID: {}", user.getImgId());
                storageService.deleteImage(user.getImgId());
            }

            StorageService.StorageResult uploadResult = storageService.uploadProcessedImage(processedImage);

            user.setImageUrl(uploadResult.getUrl());
            user.setImgId(uploadResult.getImageId());

            User savedUser = userRepository.save(user);

            log.info("Аватар успешно загружен для пользователя с ID: {}. URL: {}, финальный формат: {}",
                    userId, uploadResult.getUrl(), processedImage.getContentType());

            return userMapper.toResponse(savedUser);

        } catch (IOException e) {
            log.error("Ошибка при загрузке аватара для пользователя с ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при загрузке изображения: " + e.getMessage(), e);
        }
    }

    @Override
//    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateProfile(Long userId, UpdateProfileDto profileDto) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));


            String oldEmail = user.getEmail();

            userMapper.updateUserFromDto(user, profileDto);
            User savedUser = userRepository.save(user);

            return userMapper.toResponse(savedUser);

        } catch (RuntimeException e) {
            log.error("Ошибка при обновлении профиля для пользователя с ID {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    @Override
//    @Cacheable(value = "users", key = "#userId")
    public UserResponse getUserById(Long userId) {

        User user = userRepository.findByIdWithOnlineStatus(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        UserResponse userResponse = userMapper.toResponse(user);

        userResponse.setPostsCount(getPostsCount(userId));
        userResponse.setFollowersCount(getFollowersCount(userId));
        userResponse.setFollowingCount(getFollowingCount(userId));

        return userResponse;
    }

    private Long getFollowersCount(Long userId) {
        return subscriptionRepository.countBySubscribedToId(userId);
    }

    private Long getFollowingCount(Long userId) {
        return subscriptionRepository.countBySubscriberId(userId);
    }

    private Long getPostsCount(Long userId) {
        return postRepository.countByAuthorId(userId);
    }

    @Override
    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Пользователь с ID " + userId + " не найден");
        }
    }

    @Override
    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

        if (user.getImgId() != null && !user.getImgId().isEmpty()) {
            storageService.deleteImage(user.getImgId());
            user.setImageUrl(null);
            user.setImgId(null);
            User savedUser = userRepository.save(user);
            return userMapper.toResponse(savedUser);
        }

        return userMapper.toResponse(user);
    }

    @Override
//    @CacheEvict(value = "users", key = "#userId")
    public UserResponse uploadBackgroundImage(Long userId, MultipartFile file) {

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

            ImageConverterService.ProcessedImage processedImage = imageValidator.validateAndProcess(file);

            if (!processedImage.getContentType().equals(file.getContentType())) {
                log.info("Фоновое изображение {} было конвертировано из {} в {}",
                        file.getOriginalFilename(),
                        file.getContentType(),
                        processedImage.getContentType());
            }

            if (user.getImgBackgroundID() != null && !user.getImgBackgroundID().isEmpty()) {
                log.debug("Удаляем старое фоновое изображение с ID: {}", user.getImgBackgroundID());
                storageService.deleteImage(user.getImgBackgroundID());
            }

            StorageService.StorageResult uploadResult = storageService.uploadProcessedImage(processedImage);

            user.setImgBackground(uploadResult.getUrl());
            user.setImgBackgroundID(uploadResult.getImageId());

            User savedUser = userRepository.save(user);

            log.info("Фоновое изображение успешно загружено для пользователя с ID: {}. URL: {}, финальный формат: {}",
                    userId, uploadResult.getUrl(), processedImage.getContentType());

            return userMapper.toResponse(savedUser);

        } catch (IOException e) {
            log.error("Ошибка при загрузке фонового изображения для пользователя с ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка при загрузке фонового изображения: " + e.getMessage(), e);
        }
    }

    @Override
//    @CacheEvict(value = "users", key = "#userId")
    public UserResponse deleteBackgroundImage(Long userId) {
        log.info("Удаляем фоновое изображение для пользователя с ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден"));

        if (user.getImgBackgroundID() != null && !user.getImgBackgroundID().isEmpty()) {
            try {
                storageService.deleteImage(user.getImgBackgroundID());

                user.setImgBackground(null);
                user.setImgBackgroundID(null);

                User savedUser = userRepository.save(user);

                log.info("Фоновое изображение успешно удалено для пользователя с ID: {}", userId);
                return userMapper.toResponse(savedUser);

            } catch (Exception e) {
                log.error("Ошибка при удалении фонового изображения для пользователя с ID {}: {}", userId, e.getMessage());
                throw new RuntimeException("Ошибка при удалении фонового изображения: " + e.getMessage(), e);
            }
        } else {
            log.debug("У пользователя с ID {} нет фонового изображения для удаления", userId);
            return userMapper.toResponse(user);
        }
    }

    @Override
//    @Cacheable(value = "users-by-email", key = "#email")
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return userMapper.toResponse(user);
    }
//
//    @Cacheable(value = "user-entities", key = "'email:' + #email")
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmailWithOnlineStatus(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @Override
    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return getUserEntityByEmail(userEmail);
    }

    @Override
    public User getCurrentUserEntity(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        String email = authentication.getName();
        return getUserEntityByEmail(email);
    }

    @Override
    @Transactional
//    @CacheEvict(value = "user-status", key = "#userId")
    public void updateOnlineStatus(Long userId, boolean isOnline) {
        try {
            LocalDateTime now = LocalDateTime.now();

            int updatedRows = onlineStatusRepository.updateOnlineStatus(userId, isOnline, now);

            if (updatedRows == 0) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + userId));

                UserOnlineStatus status = UserOnlineStatus.builder()
                        .userId(userId)
                        .user(user)
                        .isOnline(isOnline)
                        .lastSeen(now)
                        .deviceType("WEB")
                        .build();

                onlineStatusRepository.save(status);
                log.info("Создан новый онлайн статус для пользователя {}: {}", userId, isOnline);
            } else {
                log.debug("Обновлен онлайн статус для пользователя {}: {}", userId, isOnline);
            }

        } catch (Exception e) {
            log.error("Ошибка при обновлении онлайн статуса для пользователя {}: {}", userId, e.getMessage());
        }
    }

//    @Cacheable(value = "user-status", key = "#userId")
    public boolean isUserOnline(Long userId) {
        return userRepository.findById(userId)
                .map(User::getIsOnline)
                .orElse(false);
    }

    public String getUserStatusText(Long userId) {
        return onlineStatusRepository.findByUserId(userId)
                .map(UserOnlineStatus::getOnlineStatusText)
                .orElse("статус неизвестен");
    }

    @Transactional
//    @Caching(evict = {
//            @CacheEvict(value = "user-status", key = "#userId"),
//            @CacheEvict(value = "user-entities", key = "#userId")
//    })
    public void updateLastActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastActivity(LocalDateTime.now());
        user.setIsOnline(true);

        userRepository.save(user);
        log.debug("⏰ Updated activity and set online for user {}", userId);
    }

    @Override
//    @CacheEvict(value = "users", key = "#userId")
    public void updateNotificationSettings(Long userId, Boolean receiveVisitNotifications) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setReceiveVisitNotifications(receiveVisitNotifications);
        userRepository.save(user);

        log.info("✅ Updated notification settings for user {}: receiveVisitNotifications={}",
                userId, receiveVisitNotifications);
    }

    @Override
//    @Cacheable(value = "user-entities", key = "#userId")
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "users", key = "#userId"),
//            @CacheEvict(value = "user-entities", key = "#userId")
//    })
    public void changePassword(Long userId, ChangePasswordDto changePasswordDto) {
        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
            throw new PasswordMismatchException("Новый пароль и подтверждение не совпадают");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        if (!passwordEncoder.matches(changePasswordDto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Неверный текущий пароль");
        }

        if (passwordEncoder.matches(changePasswordDto.getNewPassword(), user.getPassword())) {
            throw new SamePasswordException("Новый пароль должен отличаться от текущего");
        }

        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        userRepository.save(user);

        log.info("Пароль успешно изменен для пользователя с ID: {}", userId);
    }

    @Override
//    @Caching(evict = {
//            @CacheEvict(value = "users-by-email", allEntries = true),
//            @CacheEvict(value = "user-entities", allEntries = true)
//    })
    public void resetPasswordByEmail(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с email " + email + " не найден"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Пароль успешно сброшен для пользователя с email: {}", email);
    }

    @Override
//    @CacheEvict(value = "user-entities", key = "#user.id")
    public void addBalance(User user, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        log.info("Пополнен баланс пользователя {} на {}. Новый баланс: {}",
                user.getEmail(), amount, user.getBalance());
    }

    @Override
//    @CacheEvict(value = "user-entities", key = "#user.id")
    public void subtractBalance(User user, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        if (user.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Недостаточно средств на балансе");
        }

        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);

        log.info("Списано с баланса пользователя {} сумма {}. Остаток: {}",
                user.getEmail(), amount, user.getBalance());
    }

    @Override
    public BigDecimal getBalance(User user) {
        return user.getBalance();
    }

    @Override
    public boolean hasEnoughBalance(User user, BigDecimal amount) {
        return user.getBalance().compareTo(amount) >= 0;
    }

    @Override
//    @CacheEvict(value = "user-entities", key = "#user.id")
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean canViewBirthday(Long userId, Long viewerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        if (user.getId().equals(viewerId)) {
            return true;
        }

        if (user.getShowBirthday() == null || !user.getShowBirthday()) {
            return false;
        }

        return true;
    }

    @Override
    @Cacheable(value = "user-analytics", key = "'total-users'")
    public long getTotalUsersCount() {
        return userRepository.count();
    }

    @Override
    @Cacheable(value = "user-analytics", key = "'users-date-' + #date")
    public long getUsersCountByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return userRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }

    @Override
    @Cacheable(value = "user-analytics", key = "'users-from-' + #fromDate")
    public long getUsersCountFromDate(LocalDate fromDate) {
        LocalDateTime startDate = fromDate.atStartOfDay();
        return userRepository.countByCreatedAtGreaterThanEqual(startDate);
    }

    @Override
    @Cacheable(value = "user-analytics", key = "'active-users-' + #date")
    public long getActiveUsersCountByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return userRepository.countActiveUsersBetween(startOfDay, endOfDay);
    }

    @Override
    public long getUsersCountBetweenDates(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return userRepository.countByCreatedAtBetween(start, end);
    }

    @Override
    @Cacheable(value = "user-analytics", key = "'users-month-' + #year + '-' + #monthValue")
    public long getUsersCountByMonth(int year, int monthValue) {
        LocalDateTime startOfMonth = LocalDateTime.of(year, monthValue, 1, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);
        return userRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);
    }

    @Override
    @Cacheable(value = "user-analytics", key = "'online-count'")
    public long getOnlineUsersCount() {
        return userRepository.countByIsOnlineTrue();
    }

    @Override
    @Cacheable(value = "user-analytics", key = "'active-users-from-' + #fromDate")
    public long getActiveUsersCountFromDate(LocalDate fromDate) {
        LocalDateTime startDate = fromDate.atStartOfDay();
        return userRepository.countActiveUsersFromDate(startDate);
    }

    @Cacheable(value = "user-analytics", key = "'online-users-list'")
    public List<UserResponse> getOnlineUsers() {
        List<User> onlineUsers = userRepository.findByIsOnlineTrue();
        return onlineUsers.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserBriefDto convertToUserBriefDto(User user) {
        if (user == null) return null;

        UserOnlineStatus onlineStatus = user.getOnlineStatus();

        if (onlineStatus == null) {
            onlineStatus = onlineStatusRepository.findByUserId(user.getId()).orElse(null);
        }

        boolean isOnline = onlineStatus != null && onlineStatus.getIsOnline();
        String lastSeenText = onlineStatus != null ? onlineStatus.getOnlineStatusText() : "давно не был(а) в сети";

        return UserBriefDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .imageUrl(user.getImageUrl())
                .isOnline(isOnline)
                .lastSeenText(lastSeenText)
                .lastSeen(onlineStatus != null ? onlineStatus.getLastSeen() : null)
                .username(user.getFirstName())
                .isTyping(false)
                .isBlocked(false)
                .hasBlockedMe(false)
                .isFriend(false)
                .isFollowing(false)
                .isFollower(false)
                .postsCount(0L)
                .followersCount(0L)
                .followingCount(0L)
                .role(user.getRole().name())
                .displayLetter(getDisplayLetter(user))
                .build();
    }

    @Override
    public Long getUserIdByEmail(String email) {
        return userRepository.findIdByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь с email " + email + " не найден"));
    }

    @Override
    @Transactional
//    @Caching(evict = {
//            @CacheEvict(value = "user-status", key = "#userId"),
//            @CacheEvict(value = "user-entities", key = "#userId")
//    })
    public void setUserOnline(Long userId, boolean isOnline) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        log.debug("Current user status: online={}, lastActivity={}",
                user.getIsOnline(), user.getLastActivity());

        user.setIsOnline(isOnline);
        user.setLastActivity(LocalDateTime.now());

        if (!isOnline) {
            user.setLastSeen(LocalDateTime.now());
        }

        User savedUser = userRepository.save(user);
        log.debug("User {} status updated: online={}, lastActivity={}",
                userId, savedUser.getIsOnline(), savedUser.getLastActivity());
    }

    private String getDisplayLetter(User user) {
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            return user.getFirstName().substring(0, 1).toUpperCase();
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            return user.getLastName().substring(0, 1).toUpperCase();
        }
        return "?";
    }


    // Для настроек в личном кабинете
    public boolean updateProfilePrivacy(Long userId, ProfileAccessLevel accessLevel) {
        return profileAccessService.updateProfileAccessLevel(userId, userId, accessLevel);
    }

    public boolean updatePhotosPrivacy(Long userId, ProfileAccessLevel accessLevel) {
        return profileAccessService.updatePhotosAccessLevel(userId, userId, accessLevel);
    }

    public boolean updatePostsPrivacy(Long userId, ProfileAccessLevel accessLevel) {
        return profileAccessService.updatePostsAccessLevel(userId, userId, accessLevel);
    }
}