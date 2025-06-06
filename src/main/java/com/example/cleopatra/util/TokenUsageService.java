package com.example.cleopatra.util;

import org.springframework.stereotype.Service;

@Service
public class TokenUsageService {
    private final long maxTokens = 500_000; // лимит в месяц
    private long usedTokens = 0;

    public synchronized void addUsage(int tokensUsed) {
        usedTokens += tokensUsed;
    }

    public long getRemainingTokens() {
        return maxTokens - usedTokens;
    }

    public long getUsedTokens() {
        return usedTokens;
    }
}
