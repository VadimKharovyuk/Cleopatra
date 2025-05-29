package com.example.cleopatra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class QrPageController {

    @GetMapping("/qr-login")
    public String qrLoginPage() {
        return "qr-auth/qr-login";
    }
}
