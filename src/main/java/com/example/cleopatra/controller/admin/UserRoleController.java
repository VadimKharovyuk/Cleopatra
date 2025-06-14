package com.example.cleopatra.controller.admin;

import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.Role;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.service.UserBlockingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class UserRoleController {

    private final UserRepository userRepository;

    /**
     * Страница управления ролями пользователей
     */
    @GetMapping("/manage-roles")
    public String manageRolesPage(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "15") int size,
                                  @RequestParam(required = false) String search,
                                  @RequestParam(required = false) String roleFilter,
                                  Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;

        if (search != null && !search.trim().isEmpty()) {
            // Поиск пользователей
            users = findUsersBySearch(search.trim(), pageable);
        } else if (roleFilter != null && !roleFilter.isEmpty()) {
            // Фильтр по ролям
            try {
                Role role = Role.valueOf(roleFilter);
                users = userRepository.findByRole(role, pageable);
            } catch (IllegalArgumentException e) {
                users = userRepository.findAll(pageable);
            }
        } else {
            // Все пользователи
            users = userRepository.findAll(pageable);
        }

        // Конвертируем в UserResponse
        Page<UserResponse> userResponses = users.map(this::convertToUserResponse);

        model.addAttribute("users", userResponses);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userResponses.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("roleFilter", roleFilter);
        model.addAttribute("roles", Arrays.asList(Role.values()));

        return "admin/manage-roles";
    }

    /**
     * Поиск пользователей
     */
    @PostMapping("/manage-roles/search")
    public String searchUsers(@RequestParam String search,
                              @RequestParam(required = false) String roleFilter,
                              RedirectAttributes redirectAttributes) {

        redirectAttributes.addAttribute("search", search);
        if (roleFilter != null && !roleFilter.isEmpty()) {
            redirectAttributes.addAttribute("roleFilter", roleFilter);
        }

        return "redirect:/admin/users/manage-roles";
    }

    /**
     * Изменение роли пользователя
     */
    @PostMapping("/{userId}/change-role")
    @Transactional
    public String changeUserRole(@PathVariable Long userId,
                                 @RequestParam String newRole,
                                 @RequestParam(required = false) String reason,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Получаем текущего админа
            String adminEmail = authentication.getName();
            User currentAdmin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            // Находим пользователя
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Проверяем, что админ не меняет роль сам себе
            if (user.getId().equals(currentAdmin.getId())) {
                redirectAttributes.addFlashAttribute("error", "Вы не можете изменить роль самому себе!");
                return "redirect:/admin/users/manage-roles";
            }

            // Проверяем валидность новой роли
            Role oldRole = user.getRole();
            Role role;
            try {
                role = Role.valueOf(newRole);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Неверная роль: " + newRole);
                return "redirect:/admin/users/manage-roles";
            }

            // Изменяем роль
            user.setRole(role);
            userRepository.save(user);

            // Логируем изменение
            log.info("Admin {} changed role of user {} from {} to {}. Reason: {}",
                    currentAdmin.getEmail(), user.getEmail(), oldRole, role,
                    reason != null ? reason : "Не указано");

            redirectAttributes.addFlashAttribute("success",
                    String.format("Роль пользователя %s изменена с %s на %s",
                            user.getEmail(), oldRole.getDisplayName(), role.getDisplayName()));

        } catch (Exception e) {
            log.error("Error changing user role: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка изменения роли: " + e.getMessage());
        }

        return "redirect:/admin/users/manage-roles";
    }

    /**
     * Получение информации о пользователе для модального окна
     */
    @GetMapping("/{userId}/info")
    @ResponseBody
    public UserResponse getUserInfo(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return convertToUserResponse(user);
    }

    /**
     * Массовое изменение ролей
     */
    @PostMapping("/bulk-role-change")
    @Transactional
    public String bulkRoleChange(@RequestParam List<Long> userIds,
                                 @RequestParam String newRole,
                                 @RequestParam(required = false) String reason,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            String adminEmail = authentication.getName();
            User currentAdmin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            Role role = Role.valueOf(newRole);

            // Исключаем админа из списка
            List<Long> filteredUserIds = userIds.stream()
                    .filter(id -> !id.equals(currentAdmin.getId()))
                    .collect(Collectors.toList());

            if (filteredUserIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Нет пользователей для изменения роли");
                return "redirect:/admin/users/manage-roles";
            }

            // Получаем пользователей и меняем роли
            List<User> users = userRepository.findAllById(filteredUserIds);
            users.forEach(user -> user.setRole(role));
            userRepository.saveAll(users);

            log.info("Admin {} changed roles of {} users to {}. Reason: {}",
                    currentAdmin.getEmail(), users.size(), role,
                    reason != null ? reason : "Не указано");

            redirectAttributes.addFlashAttribute("success",
                    String.format("Роль изменена для %d пользователей на %s",
                            users.size(), role.getDisplayName()));

        } catch (Exception e) {
            log.error("Error in bulk role change: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка массового изменения ролей: " + e.getMessage());
        }

        return "redirect:/admin/users/manage-roles";
    }

    // Вспомогательные методы

    private Page<User> findUsersBySearch(String search, Pageable pageable) {
        // Поиск по email или имени
        return userRepository.findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                search, search, search, pageable);
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .imageUrl(user.getImageUrl())
                .isPrivateProfile(user.getIsPrivateProfile())
                .isBlocked(user.getIsBlocked())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .createdAt(user.getCreatedAt())
                .build();
    }
}