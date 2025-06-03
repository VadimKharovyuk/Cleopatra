package com.example.cleopatra.controller;
import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostListDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.service.PostService;
import com.example.cleopatra.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final UserService userService;

    /**
     * Показать форму создания поста
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("postCreateDto", new PostCreateDto());
        return "posts/create";
    }

    @PostMapping("/create")
    public String createPost(@Valid @ModelAttribute PostCreateDto postCreateDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes,
                             Model model) {

        // Проверка валидации
        if (bindingResult.hasErrors()) {
            model.addAttribute("postCreateDto", postCreateDto);
            return "posts/create";
        }


        try {
            PostResponseDto createdPost = postService.createPost(postCreateDto);

            log.info("Пост успешно создан с ID: {}", createdPost.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Пост успешно создан!");

            return "redirect:/posts/" + createdPost.getId();

        } catch (Exception e) {
            log.error("Ошибка при создании поста: {}", e.getMessage());
            model.addAttribute("errorMessage", "Ошибка при создании поста: " + e.getMessage());
            model.addAttribute("postCreateDto", postCreateDto);
            return "posts/create";
        }
    }




    @GetMapping("/{id}")
    public String showPost(@PathVariable Long id, Model model) {
        PostResponseDto post = postService.getPostById(id);
        model.addAttribute("post", post);
        return "posts/view";
    }



    /**
     * Получить посты пользователя
     */
    @GetMapping("/users/{userId}/posts")
    public String getUserPosts(@PathVariable Long userId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {

        try {
            PostListDto userPosts = postService.getUserPosts(userId, page, size);

            // Получаем информацию о владельце постов для отображения
            UserResponse userInfo = userService.getUserById(userId);

            model.addAttribute("posts", userPosts);
            model.addAttribute("userInfo", userInfo);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            log.info("Отображение постов пользователя {}: найдено {} постов на странице {}",
                    userId, userPosts.getNumberOfElements(), page);

            return "posts/user-posts";

        } catch (Exception e) {
            log.error("Ошибка при получении постов пользователя {}: {}", userId, e.getMessage());
            model.addAttribute("errorMessage", "Не удалось загрузить посты пользователя: " + e.getMessage());
            return "posts/error";
        }
    }


    @GetMapping("/my-posts")
    public String getMyPosts(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {

        try {


            // Получаем информацию о текущем пользователе
            User currentUser = userService.getCurrentUserEntity();

            PostListDto myPosts = postService.getMyPosts(page, size);
            model.addAttribute("posts", myPosts);


            model.addAttribute("userInfo", currentUser);
            model.addAttribute("isOwnProfile", true);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);

            log.info("Отображение собственных постов: найдено {} постов на странице {}",
                    myPosts.getNumberOfElements(), page);

            return "posts/user-posts";

        } catch (Exception e) {
            log.error("Ошибка при получении собственных постов: {}", e.getMessage());
            model.addAttribute("errorMessage", "Не удалось загрузить ваши посты: " + e.getMessage());
            return "posts/error";
        }
    }


    /**
     * API endpoint для создания поста (для AJAX)
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<PostResponseDto> createPostApi(@Valid @ModelAttribute PostCreateDto postCreateDto) {
        try {
            PostResponseDto createdPost = postService.createPost(postCreateDto);
            return ResponseEntity.ok(createdPost);
        } catch (Exception e) {
            log.error("Ошибка при создании поста через API: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    /**
     * API endpoint для AJAX загрузки постов пользователя
     */
    @GetMapping("/api/users/{userId}/posts")
    @ResponseBody
    public ResponseEntity<PostListDto> getUserPostsApi(@PathVariable Long userId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        try {
            PostListDto userPosts = postService.getUserPosts(userId, page, size);
            return ResponseEntity.ok(userPosts);
        } catch (Exception e) {
            log.error("Ошибка API при получении постов пользователя {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

    }
}
