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

import java.util.List;

/**
 * Контроллер для управления списком участников группы
 */
@Controller
@RequestMapping("/groups/{groupId}/members")
@RequiredArgsConstructor
@Slf4j
public class GroupMembersController {

    private final GroupMembershipService groupMembershipService;
    private final UserService userService;

    /**
     * Страница списка всех участников группы
     */
    @GetMapping
    public String showMembersList(
            @PathVariable Long groupId,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication auth,
            Model model) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return "redirect:/login";
        }

        try {
            GroupMembersListDto members = groupMembershipService.getAllGroupMembers(
                    groupId, currentUserId, roleFilter, sortBy, sortOrder, page, size);

            model.addAttribute("members", members);
            model.addAttribute("groupId", groupId);
            model.addAttribute("currentUserId", currentUserId);
            model.addAttribute("roleFilter", roleFilter);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortOrder", sortOrder);

            return "groups/members-list";

        } catch (GroupNotFoundException e) {
            log.error("Группа не найдена: {}", groupId);
            return "error/404";

        } catch (GroupAccessDeniedException e) {
            log.error("Недостаточно прав для просмотра участников группы {}: {}", groupId, e.getMessage());
            return "error/403";

        } catch (Exception e) {
            log.error("Ошибка при загрузке списка участников группы {}: {}", groupId, e.getMessage());
            model.addAttribute("errorMessage", "Ошибка при загрузке списка участников");
            return "error/500";
        }
    }

    /**
     * AJAX: Получить список всех участников группы
     */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<GroupMembersListDto> getGroupMembersApi(
            @PathVariable Long groupId,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            GroupMembersListDto members = groupMembershipService.getAllGroupMembers(
                    groupId, currentUserId, roleFilter, sortBy, sortOrder, page, size);

            log.info("Получен список участников группы {} для пользователя {}: {} участников",
                    groupId, currentUserId, members.getTotalMembers());

            return ResponseEntity.ok(members);

        } catch (GroupAccessDeniedException e) {
            log.warn("Недостаточно прав для получения списка участников группы {} пользователем {}",
                    groupId, currentUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (GroupNotFoundException e) {
            log.warn("Группа {} не найдена при запросе списка участников", groupId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception e) {
            log.error("Ошибка при получении списка участников группы {}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * AJAX: Поиск участников группы
     */
    @PostMapping("/search")
    @ResponseBody
    public ResponseEntity<GroupMembersListDto> searchGroupMembers(
            @PathVariable Long groupId,
            @RequestBody MemberSearchRequest request,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            GroupMembersListDto members = groupMembershipService.searchGroupMembers(groupId, currentUserId, request);

            log.info("Поиск участников группы {} по запросу '{}': найдено {} участников",
                    groupId, request.getQuery(), members.getTotalMembers());

            return ResponseEntity.ok(members);

        } catch (GroupAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            log.error("Ошибка при поиске участников группы {}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * AJAX: Получить краткий список участников (для автодополнения)
     */
    @GetMapping("/summary")
    @ResponseBody
    public ResponseEntity<List<GroupMemberSummaryDto>> getGroupMembersSummary(
            @PathVariable Long groupId,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<GroupMemberSummaryDto> members = groupMembershipService.getGroupMembersSummary(groupId, currentUserId);

            log.info("Получен краткий список участников группы {}: {} участников", groupId, members.size());

            return ResponseEntity.ok(members);

        } catch (GroupAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            log.error("Ошибка при получении краткого списка участников группы {}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * AJAX: Получить информацию об участнике
     */
    @GetMapping("/{userId}/info")
    @ResponseBody
    public ResponseEntity<GroupMembershipDto> getMemberInfo(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Получаем всех участников и ищем нужного
            GroupMembersListDto allMembers = groupMembershipService.getAllGroupMembers(groupId, currentUserId);

            GroupMembershipDto member = allMembers.getMembers().stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);

            if (member == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(member);

        } catch (GroupAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (Exception e) {
            log.error("Ошибка при получении информации об участнике {} группы {}: {}",
                    userId, groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Получить ID текущего пользователя
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}