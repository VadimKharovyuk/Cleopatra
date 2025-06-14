package com.example.cleopatra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/access-denied")
@Controller
public class AccessController {

    @GetMapping()
    public String accessDenied() {
        return "error/access-denied";
    }
}
