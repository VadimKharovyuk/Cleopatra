package com.example.cleopatra.controller;
import com.example.cleopatra.dto.user.RegisterDto;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.enums.Gender;
import com.example.cleopatra.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;


    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        model.addAttribute("genders", Gender.values());
        return "auth/register";
    }


    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute RegisterDto registerDto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("genders", Gender.values());
            return "auth/register";
        }

        try {
            UserResponse userResponse = userService.createUser(registerDto);

            log.info("Новый пользователь зарегистрирован: {}", userResponse.getEmail());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Регистрация прошла успешно! Добро пожаловать в группу Клеопатра!");

            return "redirect:/login";

        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя: {}", e.getMessage());

            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("genders", Gender.values());

            return "auth/register";
        }
    }


    /**
     * Отображение формы входа
     */
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        return "auth/login";
    }



}
