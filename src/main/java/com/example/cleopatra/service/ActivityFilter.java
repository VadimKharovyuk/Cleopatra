//package com.example.cleopatra.service;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.ServletRequest;
//import jakarta.servlet.ServletResponse;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.logging.Filter;
//
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class ActivityFilter implements jakarta.servlet.Filter {
//
//    private final UserService userService;
//
//    // ✅ Кэш для избежания частых обновлений БД
//    private final Map<Long, Long> lastUpdateTime = new ConcurrentHashMap<>();
//    private final long UPDATE_INTERVAL = 60000; // 1 минута
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication != null && authentication.isAuthenticated()
//                && !authentication.getName().equals("anonymousUser")) {
//
//            String requestURI = httpRequest.getRequestURI();
//            if (!requestURI.matches(".+\\.(css|js|png|jpg|jpeg|gif|ico|woff|woff2|ttf)$")) {
//
//                try {
//                    Long userId = userService.getUserIdByEmail(authentication.getName());
//
//                    // ✅ Throttling: обновляем не чаще раза в минуту
//                    Long lastUpdate = lastUpdateTime.get(userId);
//                    long currentTime = System.currentTimeMillis();
//
//                    if (lastUpdate == null || (currentTime - lastUpdate) > UPDATE_INTERVAL) {
//                        userService.setUserOnline(userId, true);
//                        lastUpdateTime.put(userId, currentTime);
//                        log.debug("Updated online status for user: {}", userId);
//                    }
//
//                } catch (Exception e) {
//                    log.debug("Error tracking activity for user: {}", authentication.getName(), e);
//                }
//            }
//        }
//
//        chain.doFilter(request, response);
//    }
//
//}