package com.example.cleopatra.util;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenUsageService tokenUsageService;

    @GetMapping("/status")
    public Map<String, Object> getTokenStatus() {
        return Map.of(
                "usedTokens", tokenUsageService.getUsedTokens(),
                "remainingTokens", tokenUsageService.getRemainingTokens()
        );
    }
}