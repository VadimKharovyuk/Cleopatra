package com.example.cleopatra.controller;
import com.example.cleopatra.dto.Post.PostCreateDto;
import com.example.cleopatra.dto.Post.PostResponseDto;
import com.example.cleopatra.service.PostService;
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

    @GetMapping("/{id}")
    public String showPost(@PathVariable Long id, Model model) {
        PostResponseDto post = postService.getPostById(id);
        model.addAttribute("post", post);
        return "posts/view";
    }
}
