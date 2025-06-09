package com.example.cleopatra.controller;

import com.example.cleopatra.dto.BlockedUse.BlockedUserDto;
import com.example.cleopatra.dto.BlockedUse.BlockedUsersPageResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.UserBlockService;
import com.example.cleopatra.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/blocks")
@RequiredArgsConstructor
@Slf4j
public class UserBlockWebController {

    private final UserBlockService userBlockService;
    private final UserService userService;

    /**
     * Страница со списком заблокированных пользователей
     */
    @GetMapping
    public String blockedUsersPage(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        User user = userService.getCurrentUserEntity(authentication);
        Pageable pageable = PageRequest.of(page, size);

        BlockedUsersPageResponse response = userBlockService.getBlockedUsersPageResponse(user, pageable);

        model.addAttribute("blockedUsersPage", response);
        model.addAttribute("currentUser", user);
        model.addAttribute("isOwnList", true);

        return "blocks/blocked-users";
    }



    /**
     * Страница статистики блокировок
     */
    @GetMapping("/stats")
    public String blockStatsPage(Authentication authentication, Model model) {
        User user = userService.getCurrentUserEntity(authentication);
        long blockedCount = userBlockService.getBlockedUsersCount(user.getId());

        model.addAttribute("blockedCount", blockedCount);
        model.addAttribute("currentUser", user);

        return "blocks/stats";
    }
}