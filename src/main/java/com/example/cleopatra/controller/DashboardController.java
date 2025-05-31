package com.example.cleopatra.controller;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.attribute.UserPrincipal;
@RequiredArgsConstructor
@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    private final UserService userService;

    @GetMapping
    public String dashboard() {

        return "user/dashboard";
    }

}
