package com.example.cleopatra.service;

import com.example.cleopatra.model.User;
import com.example.cleopatra.service.UserService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BonusService {

    private final UserService userService;


    private static final BigDecimal WELCOME_BONUS_AMOUNT = new BigDecimal("50.00");


    public boolean canClaimWelcomeBonus(User user) {
        if (user == null) {
            return false;
        }

        // Проверяем, что бонус еще не получен
        Boolean claimed = user.getWelcomeBonusClaimed();
        return claimed == null || !claimed;
    }


    @Transactional
    public boolean claimWelcomeBonus(User user) {
        log.info("Попытка начисления приветственного бонуса пользователю: {}", user.getEmail());

        // Проверяем право на получение бонуса
        if (!canClaimWelcomeBonus(user)) {
            log.warn("Пользователь {} уже получил приветственный бонус", user.getEmail());
            return false;
        }

        try {
            // Получаем текущий баланс
            BigDecimal currentBalance = user.getBalance();
            if (currentBalance == null) {
                currentBalance = BigDecimal.ZERO;
            }

            // Начисляем бонус
            BigDecimal newBalance = currentBalance.add(WELCOME_BONUS_AMOUNT);
            user.setBalance(newBalance);

            // Отмечаем, что бонус получен
            user.setWelcomeBonusClaimed(true);

            // Сохраняем изменения
            userService.save(user);

            log.info("Приветственный бонус {} успешно начислен пользователю {}. Новый баланс: {}",
                    WELCOME_BONUS_AMOUNT, user.getEmail(), newBalance);

            return true;

        } catch (Exception e) {
            log.error("Ошибка при начислении приветственного бонуса пользователю {}: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при начислении бонуса");
        }
    }


    public WelcomeBonusInfo getWelcomeBonusInfo(User user) {
        boolean canClaim = canClaimWelcomeBonus(user);
        return WelcomeBonusInfo.builder()
                .amount(WELCOME_BONUS_AMOUNT)
                .canClaim(canClaim)
                .alreadyClaimed(!canClaim)
                .build();
    }

    /**
     * DTO для информации о приветственном бонусе
     */
  @Builder
  @Data
    public static class WelcomeBonusInfo {
        private BigDecimal amount;
        private boolean canClaim;
        private boolean alreadyClaimed;
    }
}