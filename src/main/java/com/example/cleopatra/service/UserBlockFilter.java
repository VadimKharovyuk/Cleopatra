package com.example.cleopatra.service;

import com.example.cleopatra.service.UserBlockingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserBlockFilter extends OncePerRequestFilter {

    private final UserBlockingService userBlockingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("🔍 UserBlockFilter: проверяем путь {}", path);

        // Пропускаем публичные эндпоинты
        if (isPublicPath(path)) {
            log.debug("✅ Путь {} публичный, пропускаем", path);
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("🔍 Authentication: {}", authentication != null ? authentication.getName() : "null");

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

            String userEmail = authentication.getName();
            log.debug("🔍 Проверяем блокировку для пользователя: {}", userEmail);

            try {
                boolean isBlocked = userBlockingService.isUserBlocked(userEmail);
                log.debug("🔍 Пользователь {} заблокирован: {}", userEmail, isBlocked);

                if (isBlocked) {
                    log.warn("🚫 Blocked user {} attempted to access: {}", userEmail, path);

                    // Очищаем контекст безопасности
                    SecurityContextHolder.clearContext();

                    // Инвалидируем сессию
                    if (request.getSession(false) != null) {
                        request.getSession(false).invalidate();
                    }

                    // Если это AJAX запрос - возвращаем JSON
                    if (isAjaxRequest(request)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"error\": \"USER_BLOCKED\", \"message\": \"Your account has been blocked. Please contact support.\"}"
                        );
                    } else {
                        // Если обычный запрос - редирект на страницу блокировки
                        response.sendRedirect("/blocked-account");
                    }
                    return;
                }
            } catch (Exception e) {
                log.error("❌ Error checking user block status for {}: {}", userEmail, e.getMessage(), e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.equals("/") ||
                path.equals("/register") ||
                path.equals("/login") ||
                path.equals("/blocked-account") ||
                path.startsWith("/funny-login") ||
                path.equals("/qr-login") ||
                path.startsWith("/auth/") ||
                path.startsWith("/job/") ||
                path.startsWith("/health/") ||
                path.startsWith("/forgot-password/") ||
                path.startsWith("/vacancies/") ||
                path.startsWith("/devices/") ||
                path.startsWith("/api/auth/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/favicon.ico") ||
                path.equals("/error");
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String xRequestedWith = request.getHeader("X-Requested-With");
        String contentType = request.getHeader("Content-Type");
        return "XMLHttpRequest".equals(xRequestedWith) ||
                (contentType != null && contentType.contains("application/json"));
    }
}