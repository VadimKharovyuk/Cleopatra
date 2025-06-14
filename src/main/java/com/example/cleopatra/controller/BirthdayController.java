package com.example.cleopatra.controller;

import com.example.cleopatra.dto.BirthdayUser.BirthdayPageResponse;
import com.example.cleopatra.service.BirthdayService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/birthday")
public class BirthdayController {

    private final BirthdayService birthdayService;
    private final UserService userService;

    @GetMapping("/list")
    public String birthday(Model model,
                           Authentication authentication,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {

        Long currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size);
        BirthdayPageResponse birthdayPageResponse = birthdayService.getSubscriptionsBirthdays(currentUserId, pageable);

        model.addAttribute("birthdayResponse", birthdayPageResponse);
        model.addAttribute("currentPage", page);


        log.info("Отображение списка дней рождения для пользователя: {}, страница: {}", currentUserId, page);

        return "birthday/list";
    }

    /**
     * Получение ID текущего пользователя
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}