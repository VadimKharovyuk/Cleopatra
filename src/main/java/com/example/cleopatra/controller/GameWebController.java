package com.example.cleopatra.controller;
import com.example.cleopatra.game.*;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/game")
@RequiredArgsConstructor
@Slf4j
public class GameWebController {

    private final GameService gameService;
    private final UserService userService;
    private final LeaderboardService leaderboardService;



    @GetMapping("/milioner")
    public String  homePage(Model model) {
        return "game/home";
    }

    @GetMapping
    public String gamePage(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userService.getCurrentUserEntity(authentication);

        // Проверяем есть ли активная игра
        Optional<GameSessionResponse> currentGame = gameService.getCurrentGame(user);

        if (currentGame.isPresent()) {
            model.addAttribute("currentGame", currentGame.get());
            model.addAttribute("hasActiveGame", true);
        } else {
            model.addAttribute("hasActiveGame", false);
        }

        // Добавляем статистику пользователя
        model.addAttribute("userStats", leaderboardService.getUserDetailedStats(user));
        model.addAttribute("user", user);

        return "game/index";
    }

    @GetMapping("/play")
    public String playGame(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userService.getCurrentUserEntity(authentication);

        // Проверяем есть ли активная игра
        Optional<GameSessionResponse> currentGame = gameService.getCurrentGame(user);

        if (currentGame.isPresent()) {
            model.addAttribute("game", currentGame.get());
            return "game/play";
        } else {
            return "redirect:/game?needStart=true";
        }
    }

    // Добавьте эти методы в ваш GameWebController

    @GetMapping("/history")
    public String gameHistory(Model model, Authentication authentication,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String period,
                              @RequestParam(required = false) String score) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userService.getCurrentUserEntity(authentication);

        // Создаем объект Pageable для пагинации
        Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());

        // Получаем историю игр
        Page<GameHistoryDto> gamesPage = gameService.getGameHistory(user, pageable);

        // Добавляем данные в модель
        model.addAttribute("games", gamesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", gamesPage.getTotalPages());
        model.addAttribute("totalElements", gamesPage.getTotalElements());
        model.addAttribute("hasPrevious", gamesPage.hasPrevious());
        model.addAttribute("hasNext", gamesPage.hasNext());

        // Получаем статистику пользователя
        PlayerStatsDto stats = leaderboardService.getUserDetailedStats(user);
        model.addAttribute("stats", stats);

        model.addAttribute("user", user);

        return "game/history";
    }

    @GetMapping("/api/game/{sessionId}/details")
    @ResponseBody
    public ResponseEntity<GameDetailDto> getGameDetails(@PathVariable Long sessionId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            User user = userService.getCurrentUserEntity(authentication);
            GameDetailDto gameDetail = gameService.getGameDetail(sessionId, user);
            return ResponseEntity.ok(gameDetail);
        } catch (Exception e) {
            log.error("Error getting game details for session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userService.getCurrentUserEntity(authentication);

        // Рейтинг подписок
        model.addAttribute("subscriptionsLeaderboard",
                leaderboardService.getSubscriptionsLeaderboard(user, 10));

        // Глобальный рейтинг
        model.addAttribute("globalLeaderboard",
                leaderboardService.getGlobalLeaderboard(user, 20));

        model.addAttribute("user", user);

        return "game/leaderboard";
    }

    @GetMapping("/stats")
    public String playerStats(Model model, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        User user = userService.getCurrentUserEntity(authentication);

        model.addAttribute("stats", leaderboardService.getUserDetailedStats(user));
        model.addAttribute("user", user);

        return "game/stats";
    }
}
