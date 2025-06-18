package com.example.cleopatra.util;

import com.example.cleopatra.service.UserOnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TestREstController {
    private final UserOnlineStatusService userOnlineStatusService;


    @GetMapping("/test/ultra/{userId}")
    @ResponseBody
    public String ultraSimple(@PathVariable Long userId) {
        try {
            userOnlineStatusService.updateOnlineStatusUltraSimple(userId, true);
            return "✅ Ультра-простое обновление для " + userId + "!";
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    @GetMapping("/test/online/{userId}")
    @ResponseBody
    public String setOnline(@PathVariable Long userId) {
        try {
            userOnlineStatusService.setUserOnline(userId);
            return "✅ Пользователь " + userId + " онлайн!";
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }

    @GetMapping("/test/offline/{userId}")
    @ResponseBody
    public String setOffline(@PathVariable Long userId) {
        try {
            userOnlineStatusService.setUserOffline(userId);
            return "✅ Пользователь " + userId + " офлайн!";
        } catch (Exception e) {
            return "❌ Ошибка: " + e.getMessage();
        }
    }
}
