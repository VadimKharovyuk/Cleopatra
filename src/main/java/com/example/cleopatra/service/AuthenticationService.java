package com.example.cleopatra.service;

import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j

public class AuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + email));

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
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                log.warn("Authentication failed: user not found with email: {}", email);
                return Optional.empty();
            }

            User user = userOpt.get();

//

            // Если у вас пароли зашифрованы (рекомендуется)
             if (passwordEncoder.matches(password, user.getPassword())) {
                 log.info("User authenticated successfully: {}", email);
                 return Optional.of(user);
             }

            log.warn("Authentication failed: incorrect password for user: {}", email);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error during authentication for user: {}", email, e);
            return Optional.empty();
        }
    }
}
