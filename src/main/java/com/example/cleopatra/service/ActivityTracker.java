//package com.example.cleopatra.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class ActivityTracker {
//
//    private final UserService userService;
//
//    /**
//     * Вызывать при любой активности пользователя
//     */
//    public void trackActivity(Authentication authentication) {
//        if (authentication != null) {
//            try {
//                Long userId = userService.getUserIdByEmail(authentication.getName());
//                userService.updateLastActivity(userId);
//            } catch (Exception e) {
//                // Логируем, но не ломаем основной флоу
//                log.debug("Error tracking activity", e);
//            }
//        }
//    }
//}
