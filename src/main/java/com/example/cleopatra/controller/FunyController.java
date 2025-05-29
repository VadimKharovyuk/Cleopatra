package com.example.cleopatra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class FunyController {


    @GetMapping("/funny-login")
    public String funnyLogin() {
        return "auth/funny-login";
    }

    @GetMapping("/funny-login1")
    public String funnyLogin1() {
        return "auth/funny";
    }
}
