package com.example.cleopatra.controller;

import com.example.cleopatra.ExistsException.ImageValidationException;
import com.example.cleopatra.dto.Post.PostCardDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.user.UpdateProfileDto;
import com.example.cleopatra.dto.user.UserRecommendationDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.ProfileAccessLevel;
import com.example.cleopatra.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import jakarta.validation.Valid;

import java.util.*;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;
    private final RecommendationService recommendationService;
    private final ImageValidator imageValidator;
    private final SubscriptionService subscriptionService;
    private final IpAddressService ipAddressService;
    private final PostService postService;
    private final UserBlockService userBlockService;
    private final UserOnlineStatusService userOnlineStatusService;
    private final ProfileAccessService profileAccessService;

    @GetMapping("/{userId}")
    public String showProfile(@PathVariable Long userId,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "6") int size,
                              Model model,
                              HttpServletRequest request,
                              Authentication authentication) {
        try {
            // ====== ИНИЦИАЛИЗАЦИЯ БАЗОВЫХ ПЕРЕМЕННЫХ ======
            Long currentUserId = null;
            UserResponse currentUser = null;
            boolean isAuthenticated = false;

            // Debug информация
            model.addAttribute("debugUserId", userId);
            model.addAttribute("debugAuthenticated", authentication != null && authentication.isAuthenticated());

            // ====== ПОЛУЧЕНИЕ ДАННЫХ ТЕКУЩЕГО ПОЛЬЗОВАТЕЛЯ ======
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    currentUser = userService.getUserByEmail(authentication.getName());
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                        isAuthenticated = true;

                        // Обновляем онлайн статус только для собственного профиля
                        if (currentUserId.equals(userId)) {
                            userOnlineStatusService.setUserOnline(currentUserId);
                            log.debug("Обновлен онлайн статус для текущего пользователя: {}", currentUserId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Ошибка получения данных текущего пользователя: {}", e.getMessage());
                }
            }

            // ====== УСТАНОВКА БАЗОВЫХ ПЕРЕМЕННЫХ ======
            model.addAttribute("currentUserId", currentUserId);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("debugCurrentUserId", currentUserId != null ? currentUserId : "null");

            // ====== ПРОВЕРКА ДОСТУПА К ПРОФИЛЮ ======
            boolean canView = profileAccessService.canViewProfile(currentUserId, userId);

            if (!canView) {
                log.warn("🚫 ДОСТУП ЗАПРЕЩЕН! userId={}, currentUserId={}", userId, currentUserId);

                // Получаем данные заблокированного пользователя
                UserResponse blockedUser = userService.getUserById(userId);
                String accessDeniedMessage = profileAccessService.getAccessDeniedMessage(currentUserId, userId);
                ProfileAccessLevel userAccessLevel = profileAccessService.getProfileAccessLevel(userId);

                // Основные данные для страницы блокировки
                model.addAttribute("blockedUser", blockedUser);
                model.addAttribute("user", blockedUser); // ДЛЯ СОВМЕСТИМОСТИ с HTML
                model.addAttribute("accessDeniedMessage", accessDeniedMessage);
                model.addAttribute("userAccessLevel", userAccessLevel);

                // Информация о подписке (только для авторизованных)
                boolean isSubscribed = false;
                boolean isMutualSubscription = false;
                if (isAuthenticated) {
                    isSubscribed = subscriptionService.isSubscribed(currentUserId, userId);
                    isMutualSubscription = subscriptionService.isSubscribed(userId, currentUserId);
                }
                model.addAttribute("isSubscribed", isSubscribed);
                model.addAttribute("canSubscribe", !isSubscribed && isAuthenticated);

                // Информация о требованиях доступа
                boolean needsSubscription = (userAccessLevel == ProfileAccessLevel.SUBSCRIBERS_ONLY);
                boolean needsMutualSubscription = (userAccessLevel == ProfileAccessLevel.MUTUAL_SUBSCRIPTIONS);
                boolean isPrivateProfile = (userAccessLevel == ProfileAccessLevel.PRIVATE);

                model.addAttribute("needsSubscription", needsSubscription);
                model.addAttribute("needsMutualSubscription", needsMutualSubscription);
                model.addAttribute("isPrivateProfile", isPrivateProfile);

                // Debug информация
                model.addAttribute("debugAccessLevel", userAccessLevel.name());
                model.addAttribute("debugCanView", false);
                model.addAttribute("debugIsSubscribed", isSubscribed);
                model.addAttribute("debugIsMutualSubscription", isMutualSubscription);

                return "profile/CanViev-profile";
            }

            // ====== ПРОВЕРКА БЛОКИРОВОК (только для авторизованных) ======
            boolean iBlockedUser = false;
            boolean userBlockedMe = false;
            boolean canInteract = isAuthenticated;

            if (isAuthenticated && !currentUserId.equals(userId)) {
                iBlockedUser = userBlockService.isBlocked(currentUserId, userId);
                userBlockedMe = userBlockService.isBlocked(userId, currentUserId);
                canInteract = !iBlockedUser && !userBlockedMe;

                // Если просматриваемый пользователь заблокировал текущего
                if (userBlockedMe) {
                    log.warn("🚫 Пользователь {} заблокировал текущего пользователя {}", userId, currentUserId);

                    // Получаем минимальные данные для страницы блокировки
                    UserResponse blockedByUser = userService.getUserById(userId);
                    model.addAttribute("user", blockedByUser); // ДЛЯ СОВМЕСТИМОСТИ с HTML
                    model.addAttribute("isSubscribed", false);
                    model.addAttribute("iBlockedUser", iBlockedUser);
                    model.addAttribute("userBlockedMe", userBlockedMe);
                    model.addAttribute("canInteract", false);

                    return "profile/profile-blocked";
                }
            }

            model.addAttribute("iBlockedUser", iBlockedUser);
            model.addAttribute("userBlockedMe", userBlockedMe);
            model.addAttribute("canInteract", canInteract);

            // ====== ЗАГРУЗКА ДАННЫХ ПРОФИЛЯ ======
            UserResponse user = userService.getUserById(userId);
            if (user == null) {
                log.error("Пользователь с ID {} не найден", userId);
                model.addAttribute("user", null);
                model.addAttribute("isSubscribed", false);
                model.addAttribute("errorMessage", "Пользователь не найден");
                return "error/404";
            }
            model.addAttribute("user", user);

            // ====== ОПРЕДЕЛЕНИЕ ТИПА ПРОФИЛЯ ======
            boolean isOwnProfile = isAuthenticated && currentUserId.equals(userId);
            model.addAttribute("isOwnProfile", isOwnProfile);

            // ====== ИНФОРМАЦИЯ О ПОДПИСКЕ ======
            boolean isSubscribed = false;
            if (isAuthenticated && !isOwnProfile) {
                isSubscribed = subscriptionService.isSubscribed(currentUserId, userId);
            }
            model.addAttribute("isSubscribed", isSubscribed);

            // ====== ЗАГРУЗКА ПОСТОВ ======
            try {
                PostListDto userPosts = postService.getUserPosts(userId, page, size);
                model.addAttribute("posts", userPosts);
            } catch (Exception e) {
                log.warn("⚠️ Ошибка загрузки постов: {}", e.getMessage());
                model.addAttribute("posts", new PostListDto()); // Пустой объект
            }

            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            // ====== ДОПОЛНИТЕЛЬНЫЕ ДАННЫЕ (только для авторизованных) ======
            if (isAuthenticated) {
                // Записываем визит (только для чужих профилей)
                if (!isOwnProfile) {
                    try {
                        ipAddressService.recordUserVisit(userId, currentUserId, request);
                    } catch (Exception e) {
                        log.warn("⚠️ Ошибка записи визита: {}", e.getMessage());
                    }
                }

                // Рекомендации
                try {
                    List<UserRecommendationDto> recommendations = recommendationService.getTopRecommendations(currentUserId);
                    model.addAttribute("recommendations", recommendations != null ? recommendations : Collections.emptyList());
                } catch (Exception e) {
                    log.warn("⚠️ Ошибка загрузки рекомендаций: {}", e.getMessage());
                    model.addAttribute("recommendations", Collections.emptyList());
                }
            } else {
                model.addAttribute("recommendations", Collections.emptyList());
            }

            // ====== DEBUG ИНФОРМАЦИЯ ======
            model.addAttribute("debugCanView", true);
            model.addAttribute("debugAccessLevel", profileAccessService.getProfileAccessLevel(userId).name());

            String debugSubscriptionStatus;
            if (!isAuthenticated) {
                debugSubscriptionStatus = "not_authenticated";
            } else if (isOwnProfile) {
                debugSubscriptionStatus = "own_profile";
            } else {
                debugSubscriptionStatus = String.valueOf(isSubscribed);
            }
            model.addAttribute("debugIsSubscribed", debugSubscriptionStatus);

            return "profile/profile";

        } catch (Exception e) {
            log.error("❌ КРИТИЧЕСКАЯ ОШИБКА при показе профиля {}: {}", userId, e.getMessage(), e);

            // ====== УСТАНОВКА ПЕРЕМЕННЫХ ДЛЯ СТРАНИЦЫ ОШИБКИ ======
            model.addAttribute("currentUserId", null);
            model.addAttribute("currentUser", null);
            model.addAttribute("user", null);
            model.addAttribute("isSubscribed", false);
            model.addAttribute("isOwnProfile", false);
            model.addAttribute("iBlockedUser", false);
            model.addAttribute("userBlockedMe", false);
            model.addAttribute("canInteract", false);
            model.addAttribute("recommendations", Collections.emptyList());
            model.addAttribute("posts", new PostListDto());

            model.addAttribute("errorMessage", "Не удалось загрузить профиль пользователя");
            model.addAttribute("debugError", e.getMessage());

            return "error";
        }
    }

//
//
//
//    @GetMapping("/{userId}")
//    public String showProfile(@PathVariable Long userId,
//                              @RequestParam(defaultValue = "0") int page,
//                              @RequestParam(defaultValue = "6") int size,
//                              Model model,
//                              HttpServletRequest request,
//                              Authentication authentication) {
//        try {
//
//            // Добавляем debug информацию
//            model.addAttribute("debugUserId", userId);
//            model.addAttribute("debugAuthenticated", authentication != null);
//
//            if (authentication != null) {
//                UserResponse currentUser = userService.getUserByEmail(authentication.getName());
//
//                model.addAttribute("currentUserId", currentUser.getId());
//                model.addAttribute("debugCurrentUserId", currentUser.getId());
//
//
//                boolean canView = profileAccessService.canViewProfile(currentUser.getId(), userId);
//
//                // 1. Если это текущий пользователь - обновляем его онлайн статус
//                if (currentUser != null && currentUser.equals(userId)) {
//                    userOnlineStatusService.setUserOnline(currentUser.getId());
//                    log.debug("Обновлен онлайн статус для текущего пользователя: {}", currentUser);
//                }
//
//
//                if (!canView) {
//                    log.warn("🚫 ДОСТУП ЗАПРЕЩЕН! Перенаправляем на страницу блокировки");
//
//                    // Дополнительная диагностика
//                    ProfileAccessLevel userAccessLevel = profileAccessService.getProfileAccessLevel(userId);
//                    boolean isSubscribed = subscriptionService.isSubscribed(currentUser.getId(), userId);
//                    boolean isMutualSubscription = subscriptionService.isSubscribed(userId, currentUser.getId());
//
//                    // Профиль недоступен - показываем страницу блокировки
//                    UserResponse blockedUser = userService.getUserById(userId);
//                    String accessDeniedMessage = profileAccessService.getAccessDeniedMessage(currentUser.getId(), userId);
//
//                    // Основные данные для страницы блокировки
//                    model.addAttribute("blockedUser", blockedUser);
//                    model.addAttribute("accessDeniedMessage", accessDeniedMessage);
//                    model.addAttribute("currentUser", currentUser);
//
//                    // Дополнительная информация для лучшего UX
//                    model.addAttribute("userAccessLevel", userAccessLevel);
//                    model.addAttribute("isSubscribed", isSubscribed);
//                    model.addAttribute("canSubscribe", !isSubscribed);
//
//                    // Информация о том, что нужно для доступа
//                    boolean needsSubscription = (userAccessLevel == ProfileAccessLevel.SUBSCRIBERS_ONLY);
//                    boolean needsMutualSubscription = (userAccessLevel == ProfileAccessLevel.MUTUAL_SUBSCRIPTIONS);
//                    boolean isPrivateProfile = (userAccessLevel == ProfileAccessLevel.PRIVATE);
//
//                    model.addAttribute("needsSubscription", needsSubscription);
//                    model.addAttribute("needsMutualSubscription", needsMutualSubscription);
//                    model.addAttribute("isPrivateProfile", isPrivateProfile);
//
//                    // Debug информация для страницы блокировки
//                    model.addAttribute("debugAccessLevel", userAccessLevel.name());
//                    model.addAttribute("debugCanView", false);
//                    model.addAttribute("debugIsSubscribed", isSubscribed);
//                    model.addAttribute("debugIsMutualSubscription", isMutualSubscription);
//
//
//                    return "profile/CanViev-profile";
//                }
//
//                // Проверяем заблокировал ли текущий пользователь просматриваемого
//                boolean iBlockedUser = userBlockService.isBlocked(currentUser.getId(), userId);
//                // Проверяем заблокировал ли просматриваемый пользователь текущего
//                boolean userBlockedMe = userBlockService.isBlocked(userId, currentUser.getId());
//
//                model.addAttribute("iBlockedUser", iBlockedUser);
//                model.addAttribute("userBlockedMe", userBlockedMe);
//                model.addAttribute("canInteract", !iBlockedUser && !userBlockedMe);
//
//                // Если просматриваемый пользователь заблокировал текущего
//                if (userBlockedMe) {
//                    log.warn("🚫 Пользователь {} заблокировал текущего пользователя. Показываем profile-blocked", userId);
//                    return "profile/profile-blocked";
//                }
//
//                // Debug информация для успешного доступа
//                model.addAttribute("debugCanView", true);
//                model.addAttribute("debugAccessLevel", profileAccessService.getProfileAccessLevel(userId).name());
//
//            } else {
//                log.info("🔍 Неавторизованный пользователь пытается просмотреть профиль {}", userId);
//
//                // === НЕАВТОРИЗОВАННЫЙ ПОЛЬЗОВАТЕЛЬ ===
//                // Проверяем доступ для неавторизованного пользователя
//                boolean canView = profileAccessService.canViewProfile(null, userId);
//
//                if (!canView) {
//                    log.warn("🚫 Доступ запрещен для неавторизованного пользователя");
//
//                    UserResponse blockedUser = userService.getUserById(userId);
//                    String accessDeniedMessage = profileAccessService.getAccessDeniedMessage(null, userId);
//                    ProfileAccessLevel userAccessLevel = profileAccessService.getProfileAccessLevel(userId);
//
//
//                    model.addAttribute("blockedUser", blockedUser);
//                    model.addAttribute("accessDeniedMessage", accessDeniedMessage);
//                    model.addAttribute("currentUser", null);
//                    model.addAttribute("debugCanView", false);
//                    model.addAttribute("debugCurrentUserId", "anonymous");
//                    model.addAttribute("debugAccessLevel", userAccessLevel.name());
//
//                    return "profile/CanViev-profile";
//                }
//            }
//
//            // === ЗАГРУЗКА ДАННЫХ ПРОФИЛЯ (только если доступ разрешен) ===
//            UserResponse user = userService.getUserById(userId);
//            model.addAttribute("user", user);
//
//
//
//            // Получаем посты пользователя
//            PostListDto userPosts = postService.getUserPosts(userId, page, size);
//
//            model.addAttribute("posts", userPosts);
//            model.addAttribute("currentPage", page);
//            model.addAttribute("pageSize", size);
//
//            // Обрабатываем данные текущего пользователя
//            if (authentication != null && authentication.isAuthenticated()) {
//                UserResponse currentUser = userService.getUserByEmail(authentication.getName());
//                model.addAttribute("currentUserId", currentUser.getId());
//                model.addAttribute("isOwnProfile", currentUser.getId().equals(userId));
//
//
//                // Записываем визит
//                try {
//                    ipAddressService.recordUserVisit(userId, currentUser.getId(), request);
//                } catch (Exception e) {
//                    log.warn("⚠️ Ошибка записи визита: {}", e.getMessage());
//                }
//
//                // Проверяем подписку только для чужих профилей
//                if (!currentUser.getId().equals(userId)) {
//                    boolean isSubscribed = subscriptionService.isSubscribed(currentUser.getId(), userId);
//                    model.addAttribute("isSubscribed", isSubscribed);
//                    model.addAttribute("debugIsSubscribed", isSubscribed);
//                } else {
//                    model.addAttribute("isSubscribed", false);
//                    model.addAttribute("debugIsSubscribed", "own_profile");
//                }
//
//                try {
//                    List<UserRecommendationDto> recommendations = recommendationService.getTopRecommendations(currentUser.getId());
//                    model.addAttribute("recommendations", recommendations);
//
//                } catch (Exception e) {
//                    log.warn("⚠️ Ошибка загрузки рекомендаций: {}", e.getMessage());
//                }
//
//            } else {
//                model.addAttribute("currentUserId", null);
//                model.addAttribute("isSubscribed", false);
//                model.addAttribute("isOwnProfile", false);
//                model.addAttribute("debugIsSubscribed", "not_authenticated");
//            }
//
//
//            return "profile/profile";
//
//        } catch (Exception e) {
//            log.error("❌ КРИТИЧЕСКАЯ ОШИБКА при показе профиля {}: {}", userId, e.getMessage(), e);
//            model.addAttribute("errorMessage", "Не удалось загрузить профиль пользователя");
//            model.addAttribute("debugError", e.getMessage());
//            return "error";
//        }
//    }


    /**
     * AJAX загрузка постов для бесконечного скролла
     */
    @GetMapping("/{userId}/posts/api")
    @ResponseBody
    public ResponseEntity<?> getUserPostsApi(@PathVariable Long userId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "6") int size,
                                             Authentication authentication) {
        try {
            // Проверяем существование пользователя
            if (!userService.userExists(userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Пользователь не найден"));
            }

            // Проверяем доступ к профилю
            if (authentication != null && authentication.isAuthenticated()) {
                UserResponse currentUser = userService.getUserByEmail(authentication.getName());


                // Проверяем блокировки
                boolean userBlockedMe = userBlockService.isBlocked(userId, currentUser.getId());
                if (userBlockedMe) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Пользователь заблокировал вас"));
                }
            }

            // Валидация параметров
            if (page < 0) page = 0;
            if (size <= 0 || size > 20) size = 6;

            // Получаем посты
            PostListDto userPosts = postService.getUserPosts(userId, page, size);

            if (userPosts == null) {
                userPosts = PostListDto.builder()
                        .posts(new ArrayList<>())
                        .hasNext(false)
                        .hasPrevious(false)
                        .currentPage(page)
                        .pageSize(size)
                        .isEmpty(true)
                        .numberOfElements(0)
                        .build();
            }

            if (userPosts.getPosts() == null) {
                userPosts.setPosts(new ArrayList<>());
            }

            log.info("Возвращаем {} постов пользователя {} для страницы {}",
                    userPosts.getPosts().size(), userId, page);

            return ResponseEntity.ok(userPosts);

        } catch (Exception e) {
            log.error("Ошибка API загрузки постов пользователя {}: {}", userId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Внутренняя ошибка сервера");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("userId", userId);
            errorResponse.put("page", page);
            errorResponse.put("size", size);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }



    /**
     * Устаревший метод - оставляем для совместимости
     * @deprecated Используйте getUserPostsApi
     */
    @GetMapping("/{userId}/posts")
    @ResponseBody
    public ResponseEntity<PostListDto> getUserPostsAjax(@PathVariable Long userId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "6") int size) {
        try {
            PostListDto userPosts = postService.getUserPosts(userId, page, size);
            return ResponseEntity.ok(userPosts);
        } catch (Exception e) {
            log.error("Ошибка AJAX загрузки постов пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


    @GetMapping("/{userId}/edit")
    public String showEditProfile(@PathVariable Long userId, Model model) {
        try {
            UserResponse user = userService.getUserById(userId);

            // Заполняем DTO текущими данными
            UpdateProfileDto dto = new UpdateProfileDto();
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setCity(user.getCity());

            //статус
            dto.setStatusPage(user.getStatusPage());

            dto.setBirthDate(user.getBirthDate());
            dto.setShowBirthday(user.getShowBirthday());


            model.addAttribute("user", user);
            model.addAttribute("updateProfileDto", dto);


            model.addAttribute("maxFileSize", imageValidator.getMaxFileSizeMB());
            model.addAttribute("allowedFormats", imageValidator.getAllowedExtensions());
            model.addAttribute("validationRules", imageValidator.getValidationRulesDescription());

            return "profile/edit";
        } catch (RuntimeException e) {
            log.error("Ошибка при загрузке страницы редактирования профиля {}: {}", userId, e.getMessage());
            model.addAttribute("error", "Пользователь не найден");
            return "error/404";
        }
    }

    @PostMapping("/{userId}/edit")
    public String updateProfile(@PathVariable Long userId,
                                @Valid @ModelAttribute UpdateProfileDto updateDto,
                                BindingResult bindingResult,
                                Model model,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            UserResponse currentUser = userService.getUserByEmail(authentication.getName());

            if (!currentUser.getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Недостаточно прав для редактирования");
                return "redirect:/profile/" + userId;
            }

            if (bindingResult.hasErrors()) {
                UserResponse user = userService.getUserById(userId);
                model.addAttribute("user", user);
                model.addAttribute("currentUserId", currentUser.getId());


                model.addAttribute("maxFileSize", imageValidator.getMaxFileSizeMB());
                model.addAttribute("allowedFormats", imageValidator.getAllowedExtensions());
                model.addAttribute("validationRules", imageValidator.getValidationRulesDescription());

                return "profile/edit";
            }

            userService.updateProfile(userId, updateDto);
            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен");

            return "redirect:/profile/" + userId;

        } catch (Exception e) {
            log.error("Ошибка при обновлении профиля {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось обновить профиль");
            return "redirect:/profile/" + userId + "/edit";
        }
    }


    @PostMapping("/{userId}/avatar")
    public String uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("avatar") MultipartFile file,
            RedirectAttributes redirectAttributes) {


        // ✅ Минимальная проверка в контроллере
        if (file.isEmpty()) {
            log.warn("⚠️ Пустой файл аватара для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("errorMessage", "Пожалуйста, выберите файл");
            return "redirect:/profile/" + userId + "/edit";
        }

        try {
            // ✅ Основная валидация и бизнес-логика В СЕРВИСЕ
            UserResponse updatedUser = userService.uploadAvatar(userId, file);

            redirectAttributes.addFlashAttribute("successMessage", "Аватар успешно обновлен!");
            return "redirect:/profile/" + userId;

        } catch (ImageValidationException e) {
            // Специфичные ошибки валидации изображений
            log.warn("❌ Ошибка валидации аватара для пользователя {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/profile/" + userId + "/edit";

        } catch (RuntimeException e) {
            // Общие ошибки
            log.error("❌ Ошибка при загрузке аватара для пользователя {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке аватара. Попробуйте позже.");
            return "redirect:/profile/" + userId + "/edit";
        }
    }


    @PostMapping("/{userId}/background/upload")
    public String uploadBackgroundImage(
            @PathVariable Long userId,
            @RequestParam("backgroundFile") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        log.info("📤 Загрузка фона для пользователя {}: {}", userId, file.getOriginalFilename());

        if (file.isEmpty()) {
            log.warn("⚠️ Пустой файл фона для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("errorMessage", "Пожалуйста, выберите файл для загрузки");
            return "redirect:/profile/" + userId + "/edit";
        }

        try {
            userService.uploadBackgroundImage(userId, file);

            log.info("✅ Фон успешно загружен для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Фоновое изображение успешно загружено!");
            return "redirect:/profile/" + userId;

        } catch (ImageValidationException e) {
            // Специфичные ошибки валидации изображений
            log.warn("❌ Ошибка валидации фона для пользователя {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/profile/" + userId + "/edit";

        } catch (RuntimeException e) {
            log.error("❌ Ошибка при загрузке фона для пользователя {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при загрузке фонового изображения: " + e.getMessage());
            return "redirect:/profile/" + userId + "/edit";
        }
    }

    /**
     * ✅ Удаление аватара
     */
    @PostMapping("/{userId}/avatar/delete")
    public String deleteAvatar(
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {

        log.info("🗑️ Удаление аватара для пользователя {}", userId);

        try {
            userService.deleteAvatar(userId);

            log.info("✅ Аватар успешно удален для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Фотография профиля удалена!");
            return "redirect:/profile/" + userId;

        } catch (RuntimeException e) {
            log.error("❌ Ошибка при удалении аватара для пользователя {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении фотографии профиля");
            return "redirect:/profile/" + userId + "/edit";
        }
    }

    /**
     * ✅ Удаление фона
     */
    @PostMapping("/{userId}/background/delete")
    public String deleteBackground(
            @PathVariable Long userId,
            RedirectAttributes redirectAttributes) {

        log.info("🗑️ Удаление фона для пользователя {}", userId);

        try {
            userService.deleteBackgroundImage(userId);

            log.info("✅ Фон успешно удален для пользователя {}", userId);
            redirectAttributes.addFlashAttribute("successMessage", "Фоновое изображение удалено!");
            return "redirect:/profile/" + userId;

        } catch (RuntimeException e) {
            log.error("❌ Ошибка при удалении фона для пользователя {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении фонового изображения");
            return "redirect:/profile/" + userId + "/edit";
        }
    }







}