package com.example.cleopatra.ExceptionHandler;

import com.example.cleopatra.ExistsException.ImageUploadException;
import com.example.cleopatra.ExistsException.ImageValidationException;
import com.example.cleopatra.ExistsException.JobApplicationSaveException;
import com.example.cleopatra.dto.JobApplication.CreateJobApplicationDto;
import com.example.cleopatra.enums.PerformerProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Обработка ошибок валидации изображений
     */
    @ExceptionHandler(ImageValidationException.class)
    public String handleImageValidationException(
            ImageValidationException e,
            Model model,
            HttpServletRequest request) {

        log.warn("Ошибка валидации изображения: {} | URL: {}", e.getMessage(), request.getRequestURI());

        return handleJobApplicationFormError(model, e.getMessage(), "profilePicture");
    }

    /**
     * Обработка ошибок загрузки изображений
     */
    @ExceptionHandler(ImageUploadException.class)
    public String handleImageUploadException(
            ImageUploadException e,
            Model model,
            HttpServletRequest request) {

        log.error("Ошибка загрузки изображения: {} | URL: {}", e.getMessage(), request.getRequestURI());

        return handleJobApplicationFormError(model,
                "Не удалось загрузить изображение. Попробуйте еще раз или выберите другой файл.",
                "profilePicture");
    }

    /**
     * Обработка ошибок сохранения заявки
     */
    @ExceptionHandler(JobApplicationSaveException.class)
    public String handleJobApplicationSaveException(
            JobApplicationSaveException e,
            Model model,
            HttpServletRequest request) {

        log.error("Ошибка сохранения заявки: {} | URL: {}", e.getMessage(), request.getRequestURI());

        return handleJobApplicationFormError(model,
                "Произошла ошибка при сохранении заявки. Попробуйте еще раз.",
                null);
    }

    /**
     * Обработка ошибок превышения размера загружаемого файла
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e,
            Model model,
            HttpServletRequest request) {

        log.warn("Превышен максимальный размер файла: {} | URL: {}", e.getMessage(), request.getRequestURI());

        return handleJobApplicationFormError(model,
                "Размер файла слишком большой. Максимальный размер: 5MB",
                "profilePicture");
    }

    /**
     * Обработка ошибок валидации формы
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationException(
            MethodArgumentNotValidException e,
            Model model,
            HttpServletRequest request) {

        log.warn("Ошибки валидации формы: {} | URL: {}", e.getMessage(), request.getRequestURI());

        BindingResult bindingResult = e.getBindingResult();

        model.addAttribute("jobApplication", bindingResult.getTarget());
        model.addAttribute("performerProfiles", PerformerProfile.values());
        model.addAttribute("org.springframework.validation.BindingResult.jobApplication", bindingResult);

        return "job/create-form";
    }


    /**
     * Обработка IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(
            IllegalArgumentException e,
            Model model,
            HttpServletRequest request) {

        log.warn("Некорректные данные: {} | URL: {}", e.getMessage(), request.getRequestURI());

        return handleJobApplicationFormError(model,
                "Некорректные данные. Проверьте введенную информацию.",
                null);
    }

    /**
     * Универсальный метод для обработки ошибок в форме заявки
     */
    private String handleJobApplicationFormError(Model model, String errorMessage, String fieldName) {
        // Добавляем сообщение об ошибке
        if (fieldName != null) {
            model.addAttribute("fieldError_" + fieldName, errorMessage);
        } else {
            model.addAttribute("errorMessage", errorMessage);
        }

        // Добавляем необходимые атрибуты для формы
        if (!model.containsAttribute("jobApplication")) {
            model.addAttribute("jobApplication", new CreateJobApplicationDto());
        }

        if (!model.containsAttribute("performerProfiles")) {
            model.addAttribute("performerProfiles", PerformerProfile.values());
        }

        return "job/create-form";
    }

    /**
     * Метод для создания redirect с сообщением об ошибке
     */
    private String createErrorRedirect(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:/job/create";
    }
}
