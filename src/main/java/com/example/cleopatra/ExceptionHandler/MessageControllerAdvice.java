package com.example.cleopatra.ExceptionHandler;

import com.example.cleopatra.ExistsException.MessageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class MessageControllerAdvice {

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotFound(MessageNotFoundException e) {
        log.warn("Сообщение не найдено: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "MESSAGE_NOT_FOUND");
        response.put("message", e.getMessage());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        log.warn("Доступ запрещен: {}", e.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "ACCESS_DENIED");
        response.put("message", e.getMessage());
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("error", "VALIDATION_ERROR");
        response.put("message", "Ошибка валидации данных");
        response.put("fieldErrors", fieldErrors);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception e) {
//        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INTERNAL_SERVER_ERROR");
        response.put("message", "Произошла внутренняя ошибка сервера");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
