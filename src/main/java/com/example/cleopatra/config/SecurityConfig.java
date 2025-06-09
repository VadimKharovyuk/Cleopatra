package com.example.cleopatra.config;
import com.example.cleopatra.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationService authenticationService;

    @Value("${security.rememberme.key}")
    private String rememberMeKey;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf.disable())


                .authorizeHttpRequests(authz -> authz
                        // Публичные эндпоинты
                        .requestMatchers("/",
                                "/register",
                                "/login",
                                "/funny-login",
                                "/funny-login1",
                                "/qr-login",
                                "/auth/**",
                                "/admin/**",
                                "/job/**",
                                "/health/**",
                                "/forgot-password/**",
                                "/vacancies/**",
                                "/devices/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // API эндпоинты - порядок важен!
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/subscriptions/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/notifications/**").authenticated()

                        .requestMatchers("/support/**").authenticated()


                        // Эндпоинты только для админов
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Эндпоинты для артистов и админов
                        .requestMatchers("/performer/**").hasAnyRole("PERFORMER", "ADMIN")

                        // Остальные требуют аутентификации
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(60 * 60 * 24 * 14) // 14 дней
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .userDetailsService(authenticationService);

        return http.build();
    }
}