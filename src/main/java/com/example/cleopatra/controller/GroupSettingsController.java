package com.example.cleopatra.controller;

import com.example.cleopatra.ExistsException.GroupAccessDeniedException;
import com.example.cleopatra.ExistsException.GroupNotFoundException;
import com.example.cleopatra.dto.GroupDto.*;
import com.example.cleopatra.service.GroupMembershipService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Контроллер для управления настройками группы и участниками
 */
@Controller
@RequestMapping("/groups/{groupId}/settings")
@RequiredArgsConstructor
@Slf4j
public class GroupSettingsController {

    private final GroupMembershipService  groupMembershipService ;
    private final UserService userService;

    /**
     * Страница настроек группы
     */
    @GetMapping
    public String showGroupSettings(
            @PathVariable Long groupId,
            Authentication auth,
            Model model) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return "redirect:/login";
        }

        try {
            GroupSettingsDto settings = groupMembershipService.getGroupSettings(groupId, currentUserId);

            model.addAttribute("settings", settings);
            model.addAttribute("groupId", groupId);
            model.addAttribute("currentUserId", currentUserId);

            return "groups/group-settings";

        } catch (GroupNotFoundException e) {
            log.error("Группа не найдена: {}", groupId);
            return "error/404";

        } catch (GroupAccessDeniedException e) {
            log.error("Недостаточно прав для доступа к настройкам группы {}: {}", groupId, e.getMessage());
            return "error/403";

        } catch (Exception e) {
            log.error("Ошибка при загрузке настроек группы {}: {}", groupId, e.getMessage());
            model.addAttribute("errorMessage", "Ошибка при загрузке настроек группы");
            return "error/500";
        }
    }

    // ==================== MEMBERSHIP ACTIONS ====================

    /**
     * AJAX: Одобрить заявку на вступление
     */
    @PostMapping("/memberships/{membershipId}/approve")
    @ResponseBody
    public ResponseEntity<MembershipActionResponse> approveMembership(
            @PathVariable Long groupId,
            @PathVariable Long membershipId,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Необходимо войти в систему"));
        }

        try {
            MembershipActionResponse response = groupMembershipService.approveMembership(membershipId, currentUserId);
            log.info("Заявка {} одобрена пользователем {} в группе {}", membershipId, currentUserId, groupId);
            return ResponseEntity.ok(response);

        } catch (GroupAccessDeniedException e) {
            log.warn("Недостаточно прав для одобрения заявки {} пользователем {}", membershipId, currentUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Недостаточно прав"));

        } catch (IllegalStateException e) {
            log.warn("Некорректное состояние заявки {}: {}", membershipId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Ошибка при одобрении заявки {}: {}", membershipId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка при обработке заявки"));
        }
    }

    /**
     * AJAX: Отклонить заявку на вступление
     */
    @PostMapping("/memberships/{membershipId}/reject")
    @ResponseBody
    public ResponseEntity<MembershipActionResponse> rejectMembership(
            @PathVariable Long groupId,
            @PathVariable Long membershipId,
            @RequestParam(required = false) String reason,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Необходимо войти в систему"));
        }

        try {
            MembershipActionResponse response = groupMembershipService.rejectMembership(membershipId, currentUserId, reason);
            log.info("Заявка {} отклонена пользователем {} в группе {} по причине: {}",
                    membershipId, currentUserId, groupId, reason);
            return ResponseEntity.ok(response);

        } catch (GroupAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Недостаточно прав"));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Ошибка при отклонении заявки {}: {}", membershipId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка при обработке заявки"));
        }
    }

    // ==================== MEMBER MANAGEMENT ====================

    /**
     * AJAX: Заблокировать участника
     */
    @PostMapping("/memberships/{membershipId}/ban")
    @ResponseBody
    public ResponseEntity<MembershipActionResponse> banMember(
            @PathVariable Long groupId,
            @PathVariable Long membershipId,
            @RequestBody BanMemberRequest request,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Необходимо войти в систему"));
        }

        try {
            request.setMembershipId(membershipId);
            MembershipActionResponse response = groupMembershipService.banMember(request, currentUserId);
            log.info("Участник заблокирован в группе {} пользователем {} по причине: {}",
                    groupId, currentUserId, request.getReason());
            return ResponseEntity.ok(response);

        } catch (GroupAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Недостаточно прав"));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Ошибка при блокировке участника {}: {}", membershipId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка при блокировке участника"));
        }
    }

    /**
     * AJAX: Разблокировать участника
     */
    @PostMapping("/memberships/{membershipId}/unban")
    @ResponseBody
    public ResponseEntity<MembershipActionResponse> unbanMember(
            @PathVariable Long groupId,
            @PathVariable Long membershipId,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Необходимо войти в систему"));
        }

        try {
            MembershipActionResponse response = groupMembershipService.unbanMember(membershipId, currentUserId);
            log.info("Участник разблокирован в группе {} пользователем {}", groupId, currentUserId);
            return ResponseEntity.ok(response);

        } catch (GroupAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Недостаточно прав"));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Ошибка при разблокировке участника {}: {}", membershipId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка при разблокировке участника"));
        }
    }

    /**
     * AJAX: Изменить роль участника
     */
    @PostMapping("/memberships/{membershipId}/change-role")
    @ResponseBody
    public ResponseEntity<MembershipActionResponse> changeRole(
            @PathVariable Long groupId,
            @PathVariable Long membershipId,
            @RequestBody ChangeRoleRequest request,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Необходимо войти в систему"));
        }

        try {
            request.setMembershipId(membershipId);
            MembershipActionResponse response = groupMembershipService.changeRole(request, currentUserId);
            log.info("Роль участника изменена в группе {} пользователем {} на роль: {}",
                    groupId, currentUserId, request.getNewRole());
            return ResponseEntity.ok(response);

        } catch (GroupAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Недостаточно прав"));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Ошибка при изменении роли участника {}: {}", membershipId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка при изменении роли"));
        }
    }

    /**
     * AJAX: Исключить участника
     */
    @PostMapping("/memberships/{membershipId}/remove")
    @ResponseBody
    public ResponseEntity<MembershipActionResponse> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long membershipId,
            @RequestParam(required = false) String reason,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Необходимо войти в систему"));
        }

        try {
            MembershipActionResponse response = groupMembershipService.removeMember(membershipId, currentUserId, reason);
            log.info("Участник исключен из группы {} пользователем {} по причине: {}",
                    groupId, currentUserId, reason);
            return ResponseEntity.ok(response);

        } catch (GroupAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Недостаточно прав"));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("Ошибка при исключении участника {}: {}", membershipId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка при исключении участника"));
        }
    }

    // ==================== BULK ACTIONS ====================

    /**
     * AJAX: Массовые действия с заявками
     */
    @PostMapping("/memberships/bulk-action")
    @ResponseBody
    public ResponseEntity<?> bulkMembershipAction(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Необходимо войти в систему"));
        }

        try {
            // TODO: Реализовать массовые действия (принять все, отклонить все и т.д.)
            return ResponseEntity.ok(Map.of("message", "Функция массовых действий будет реализована позже"));

        } catch (Exception e) {
            log.error("Ошибка при выполнении массового действия в группе {}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при выполнении действия"));
        }
    }

    // ==================== GROUP MANAGEMENT ====================

    /**
     * AJAX: Получить статистику группы
     */
    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<?> getGroupStats(
            @PathVariable Long groupId,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Необходимо войти в систему"));
        }

        try {
            GroupSettingsDto settings = groupMembershipService.getGroupSettings(groupId, currentUserId);

            Map<String, Object> stats = Map.of(
                    "totalMembers", settings.getTotalMembers(),
                    "pendingRequests", settings.getPendingRequests(),
                    "bannedMembers", settings.getBannedMembersCount() // Обновлено имя метода
            );

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Ошибка при получении статистики группы {}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении статистики"));
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Получить ID текущего пользователя
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }

    /**
     * Создать ответ с ошибкой
     */
    private MembershipActionResponse createErrorResponse(String message) {
        return MembershipActionResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}