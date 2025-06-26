package com.example.cleopatra.controller;

import com.example.cleopatra.ExistsException.GroupAccessDeniedException;
import com.example.cleopatra.ExistsException.GroupNotFoundException;
import com.example.cleopatra.dto.GroupDto.*;
import com.example.cleopatra.service.GroupPostService;
import com.example.cleopatra.service.GroupService;
import com.example.cleopatra.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import java.util.Map;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final GroupPostService groupPostService;


    @GetMapping("/{groupId}")
    public String showGroupDetails(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth,
            Model model) {

        Long currentUserId = getCurrentUserId(auth);

        try {

            GroupResponseDTO group = groupService.getGroupById(groupId, currentUserId);


            GroupPostsSliceResponse posts = groupPostService.getGroupPosts(groupId, currentUserId, page, size);


            model.addAttribute("group", group);
            model.addAttribute("groupId", groupId);
            model.addAttribute("posts", posts);
            model.addAttribute("currentPage", page);
            model.addAttribute("currentUserId", currentUserId);
            model.addAttribute("createPostRequest", new CreateGroupPostRequest());

            return "groups/group-details";

        } catch (Exception e) {
            log.error("Ошибка при получении группы ID {}: {}", groupId, e.getMessage());
            model.addAttribute("errorMessage", "Группа не найдена или недоступна");
            return "error/404";
        }
    }
    /**
     * AJAX: Создание поста в группе (для fetch запросов)
     */
    @PostMapping("/{groupId}/posts")
    @ResponseBody
    public ResponseEntity<?> createPost(
            @PathVariable Long groupId,
            @RequestParam(required = false) String text,
            @RequestParam(required = false) MultipartFile image,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Необходимо войти в систему"));
        }



        try {
            // Создаем DTO запроса
            CreateGroupPostRequest request = CreateGroupPostRequest.builder()
                    .groupId(groupId)
                    .text(text)
                    .imageUrl(image)
                    .build();

            // Создаем пост
            GroupPostResponse response = groupPostService.createPost(request, currentUserId);


            // Возвращаем успешный ответ с данными поста
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Пост успешно создан!",
                    "post", response
            ));

        } catch (IllegalArgumentException e) {
            log.warn("AJAX ошибка валидации при создании поста: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));

        } catch (Exception e) {
            log.error("AJAX ошибка при создании поста в группе {}: {}", groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Не удалось создать пост. Попробуйте еще раз."
                    ));
        }
    }


    @PostMapping("/{groupId}/posts/{postId}/delete")
    public String deletePost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = getCurrentUserId(auth);
        log.info("Удаление поста {} пользователем {}", postId, currentUserId);

        try {
            groupPostService.deletePost(postId, currentUserId);
            redirectAttributes.addFlashAttribute("success", "Пост успешно удален!");


        } catch (RuntimeException e) {
            log.warn("Ошибка при удалении поста {}: {}", postId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Не удалось удалить пост: " + e.getMessage());

        } catch (Exception e) {
            log.error("Ошибка при удалении поста {}: {}", postId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Произошла ошибка при удалении поста.");
        }

        return "redirect:/groups/" + groupId;
    }

    /**
     * AJAX: Загрузка дополнительных постов (для бесконечной прокрутки)
     */
    @GetMapping("/{groupId}/posts/load-more")
    @ResponseBody
    public ResponseEntity<GroupPostsSliceResponse> loadMorePosts(
            @PathVariable Long groupId,
            @RequestParam int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {

        Long currentUserId = getCurrentUserId(auth);

        try {
            GroupPostsSliceResponse posts = groupPostService.getGroupPosts(groupId, currentUserId, page, size);
            return ResponseEntity.ok(posts);

        } catch (Exception e) {
            log.error("Ошибка при загрузке постов: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping
    public String showAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        log.info("Отображение публичных групп, страница: {}, размер: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        GroupPageResponse groupPage = groupService.getAllPublicGroups(pageable);

        model.addAttribute("groupPage", groupPage);
        model.addAttribute("currentPage", "public-groups");

        return "groups/groups-list";
    }


    @GetMapping("/my")
    public String showMyGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth,
            Model model) {

        Long currentUserId = getCurrentUserId(auth);
        log.info("Отображение групп пользователя ID: {}, страница: {}", currentUserId, page);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        GroupPageResponse groupPage = groupService.getAllAvailableGroups(pageable, currentUserId);

        model.addAttribute("groupPage", groupPage);
        model.addAttribute("currentPage", "my-groups");

        return "groups/groups-list";
    }

    /**
     * Страница создания группы
     */
    @GetMapping("/create")
    public String showCreateGroupForm(Model model) {
        log.info("Отображение формы создания группы");

        model.addAttribute("createGroupRequestDTO", new CreateGroupRequestDTO());

        return "groups/create-group";
    }

    /**
     * Создание новой группы
     */
    @PostMapping("/create")
    public String createGroup(
            @Valid @ModelAttribute CreateGroupRequestDTO createGroupRequestDTO,
            BindingResult bindingResult,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации при создании группы: {}", bindingResult.getAllErrors());
            model.addAttribute("createGroupRequestDTO", createGroupRequestDTO);
            return "groups/create-group";
        }

        try {
            Long currentUserId = getCurrentUserId(auth);
            log.info("Создание группы '{}' пользователем ID: {}",
                    createGroupRequestDTO.getName(), currentUserId);

            GroupResponseDTO createdGroup = groupService.createGroup(createGroupRequestDTO, currentUserId);

            log.info("Группа '{}' успешно создана с ID: {}",
                    createdGroup.getName(), createdGroup.getId());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Группа '" + createdGroup.getName() + "' успешно создана!");

            return "redirect:/groups/" + createdGroup.getId();

        } catch (Exception e) {
            log.error("Ошибка при создании группы: {}", e.getMessage());
            model.addAttribute("errorMessage", "Ошибка при создании группы: " + e.getMessage());
            model.addAttribute("createGroupRequestDTO", createGroupRequestDTO);
            return "groups/create-group";
        }
    }




    @PostMapping("/delete/{groupId}")
    public String deleteGroupById(
            @PathVariable Long groupId,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = getCurrentUserId(auth);
        if (currentUserId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Необходимо войти в систему");
            return "redirect:/login";
        }

        log.info("Попытка удаления группы ID: {} пользователем ID: {}", groupId, currentUserId);

        try {
            groupService.deleteGroup(groupId, currentUserId);

            redirectAttributes.addFlashAttribute("successMessage", "Группа успешно удалена");
            log.info("Группа ID: {} успешно удалена пользователем ID: {}", groupId, currentUserId);

            return "redirect:/groups";

        } catch (GroupNotFoundException e) {
            log.error("Группа не найдена при удалении ID {}: {}", groupId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Группа не найдена");
            return "redirect:/groups";

        } catch (GroupAccessDeniedException e) {
            log.error("Недостаточно прав для удаления группы ID {}: {}", groupId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Недостаточно прав для удаления группы");
            return "redirect:/groups/" + groupId;

        } catch (Exception e) {
            log.error("Ошибка при удалении группы ID {}: {}", groupId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении группы: " + e.getMessage());
            return "redirect:/groups/" + groupId;
        }
    }

    /**
     * Получение ID текущего пользователя из Authentication
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userService.getUserIdByEmail(authentication.getName());
    }
}