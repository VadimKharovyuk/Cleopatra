package com.example.cleopatra.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BlockedAccountController {

    @GetMapping("/blocked-account")
    public String blockedAccount(Model model) {
        model.addAttribute("title", "Аккаунт заблокирован");
        return "profile/blocked-account";
    }
}