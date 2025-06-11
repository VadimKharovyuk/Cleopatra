
package com.example.cleopatra.service;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class AuthenticationService implements UserDetailsService {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + email));
//
//        if (user.getPassword() == null || user.getPassword().isEmpty()) {
//            log.error("User {} has empty password", email);
//            throw new IllegalStateException("User has no password set");
//        }
//
//        return org.springframework.security.core.userdetails.User.builder()
//                .username(user.getEmail())
//                .password(user.getPassword())
//                .authorities("ROLE_" + user.getRole().name())
//                .build();
//    }
//
//    public Optional<User> authenticate(String email, String password) {
//        log.info("🔍 === AuthenticationService.authenticate() ===");
//        log.info("🔍 Email: {}", email);
//
//        try {
//            // Поиск пользователя
//            log.info("🔍 Поиск пользователя в БД");
//            Optional<User> userOpt = userRepository.findByEmail(email);
//
//            if (userOpt.isEmpty()) {
//
//                return Optional.empty();
//            }
//
//            User user = userOpt.get();
//
//            // Проверка пароля
//            log.info("🔍 Проверка пароля");
//            if (passwordEncoder.matches(password, user.getPassword())) {
//                log.info("✅ Пароль верный");
//                return Optional.of(user);
//            } else {
//                log.warn("🔒 Неверный пароль для email: {}", email);
//                return Optional.empty();
//            }
//
//        } catch (Exception e) {
//            log.error("❌ ОШИБКА в authenticate(): {}", e.getMessage(), e);
//            throw new RuntimeException("Ошибка аутентификации: " + e.getMessage(), e);
//        }
//    }
//}


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + email));

        // Проверяем блокировку пользователя
        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            log.warn("🚫 Blocked user {} attempted to login", email);
            throw new UsernameNotFoundException("User account is blocked");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            log.error("User {} has empty password", email);
            throw new IllegalStateException("User has no password set");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }

    public Optional<User> authenticate(String email, String password) {
        log.info("🔍 === AuthenticationService.authenticate() ===");
        log.info("🔍 Email: {}", email);

        try {
            // Поиск пользователя
            log.info("🔍 Поиск пользователя в БД");
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return Optional.empty();
            }

            User user = userOpt.get();

            // Инициализируем поле isBlocked если оно null (для старых пользователей)
            if (user.getIsBlocked() == null) {
                log.info("🔧 Инициализируем поле isBlocked для пользователя {}", email);
                user.setIsBlocked(false);
                userRepository.save(user);
            }

            // Проверяем, не заблокирован ли пользователь
            if (Boolean.TRUE.equals(user.getIsBlocked())) {
                log.warn("🚫 Blocked user {} attempted to login", email);
                return Optional.empty();
            }

            // Проверка пароля
            log.info("🔍 Проверка пароля");
            if (passwordEncoder.matches(password, user.getPassword())) {
                log.info("✅ Пароль верный");
                return Optional.of(user);
            } else {
                log.warn("🔒 Неверный пароль для email: {}", email);
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("❌ ОШИБКА в authenticate(): {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка аутентификации: " + e.getMessage(), e);
        }
    }
}