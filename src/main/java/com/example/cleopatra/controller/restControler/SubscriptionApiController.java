package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.SubscriptionService;
import com.example.cleopatra.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionApiController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    /**
     * Подписаться на пользователя
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@Valid @RequestBody SubscribeRequest request,
                                                         Authentication authentication) {
        try {
            // Проверяем что пользователь аутентифицирован
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Необходима авторизация");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            log.debug("Запрос на подписку: {} -> {}",
                    authentication.getName(), request.getSubscribedToId());

            UserResponse currentUser = getUserFromAuthentication(authentication);

            boolean success = subscriptionService.subscribe(currentUser.getId(), request.getSubscribedToId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Подписка успешно оформлена" : "Не удалось подписаться");

            if (success) {
                UserResponse subscribedToUser = userService.getUserById(request.getSubscribedToId());
                response.put("followersCount", subscribedToUser.getFollowersCount());
                response.put("isSubscribed", true);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при подписке: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Произошла ошибка при подписке");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Новый метод
    private UserResponse getUserFromUserDetails(UserDetails userDetails) {
        String email = userDetails.getUsername(); // email пользователя
        return userService.getUserByEmail(email);
    }

    /**
     * Отписаться от пользователя
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody SubscribeRequest request,
                                                           Authentication authentication) {
        try {
            log.debug("Запрос на отписку: {} -> {}",
                    authentication.getName(), request.getSubscribedToId());

           UserResponse currentUser = getUserFromAuthentication(authentication);

            boolean success = subscriptionService.unsubscribe(currentUser.getId(), request.getSubscribedToId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Отписка успешно выполнена" : "Не удалось отписаться");

            if (success) {
                // Возвращаем обновленные счетчики
               UserResponse subscribedToUser = userService.getUserById(request.getSubscribedToId());
                response.put("followersCount", subscribedToUser.getFollowersCount());
                response.put("isSubscribed", false);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при отписке: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Произошла ошибка при отписке");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    /**
     * Переключить подписку (подписаться/отписаться)
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleSubscription(@RequestBody SubscribeRequest request,
                                                                  Authentication authentication) {
        try {
            log.debug("Запрос на переключение подписки: {} -> {}",
                    authentication.getName(), request.getSubscribedToId());

           UserResponse currentUser = getUserFromAuthentication(authentication);

            boolean success = subscriptionService.toggleSubscription(currentUser.getId(), request.getSubscribedToId());
            boolean isSubscribed = subscriptionService.isSubscribed(currentUser.getId(), request.getSubscribedToId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("isSubscribed", isSubscribed);
            response.put("message", isSubscribed ? "Подписка оформлена" : "Отписка выполнена");

            if (success) {
                // Возвращаем обновленные счетчики
                UserResponse subscribedToUser = userService.getUserById(request.getSubscribedToId());
                response.put("followersCount", subscribedToUser.getFollowersCount());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при переключении подписки: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Произошла ошибка");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    /**
     * Проверить статус подписки
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(@PathVariable Long userId,
                                                                     Authentication authentication) {
        try {
            UserResponse currentUser = getUserFromAuthentication(authentication);

            boolean isSubscribed = subscriptionService.isSubscribed(currentUser.getId(), userId);

            Map<String, Object> response = new HashMap<>();
            response.put("isSubscribed", isSubscribed);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при проверке статуса подписки: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    private UserResponse getUserFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        return userService.getUserByEmail(email);

    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscribeRequest {
        private Long subscribedToId;
    }
}
