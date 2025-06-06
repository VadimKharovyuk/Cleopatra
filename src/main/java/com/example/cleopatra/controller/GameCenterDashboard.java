package com.example.cleopatra.controller;

import com.example.cleopatra.game.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/game-center")
public class GameCenterDashboard {
    private final GameService gameService;


    @GetMapping
    public String gameCenterDashboard(Model model) {
        return "game-center/dashboard";
    }
}
