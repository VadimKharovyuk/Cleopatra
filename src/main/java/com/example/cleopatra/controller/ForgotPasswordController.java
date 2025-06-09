package com.example.cleopatra.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/forgot-password")
public class ForgotPasswordController {

    @GetMapping
    public String forgotPassword(Model model) {
        return "auth/forgot-password";
    }
}
