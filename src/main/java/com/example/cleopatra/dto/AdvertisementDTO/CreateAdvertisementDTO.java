package com.example.cleopatra.dto.AdvertisementDTO;
import com.example.cleopatra.enums.AdCategory;
import com.example.cleopatra.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdvertisementDTO {

    @NotBlank(message = "Название обязательно")
    @Size(min = 5, max = 100, message = "Название должно быть от 5 до 100 символов")
    private String title;

    @NotBlank(message = "Описание обязательно")
    @Size(min = 10, max = 500, message = "Описание должно быть от 10 до 500 символов")
    private String description;

    @NotBlank(message = "Краткое описание обязательно")
    @Size(min = 5, max = 150, message = "Краткое описание должно быть от 5 до 150 символов")
    private String shortDescription;

    @NotBlank(message = "URL обязателен")
    @Pattern(regexp = "^https?://.*", message = "URL должен начинаться с http:// или https://")
    private String url;

    // Финансовые поля
    @NotNull(message = "Общий бюджет обязателен")
    @DecimalMin(value = "1.00", message = "Минимальный бюджет 1.00")
    @DecimalMax(value = "100000.00", message = "Максимальный бюджет 100,000.00")
    @Digits(integer = 8, fraction = 2, message = "Неверный формат бюджета")
    private BigDecimal totalBudget;

    @DecimalMin(value = "0.01", message = "Минимальная стоимость просмотра 0.01")
    @DecimalMax(value = "10.00", message = "Максимальная стоимость просмотра 10.00")
    @Digits(integer = 2, fraction = 2)
    private BigDecimal costPerView;

    @DecimalMin(value = "0.01", message = "Минимальная стоимость клика 0.01")
    @DecimalMax(value = "50.00", message = "Максимальная стоимость клика 50.00")
    @Digits(integer = 2, fraction = 2)
    private BigDecimal costPerClick;

    // Таргетинг
    private Gender targetGender; // может быть null = для всех

    @Min(value = 13, message = "Минимальный возраст 13 лет")
    @Max(value = 100, message = "Максимальный возраст 100 лет")
    private Integer minAge;

    @Min(value = 13, message = "Минимальный возраст 13 лет")
    @Max(value = 100, message = "Максимальный возраст 100 лет")
    private Integer maxAge;

    private String targetCity;

    @NotNull(message = "Категория обязательна")
    private AdCategory category;

    // Время показа
    private LocalTime startTime;
    private LocalTime endTime;

    @NotNull(message = "Дата начала обязательна")
    @Future(message = "Дата начала должна быть в будущем")
    private LocalDate startDate;

    @NotNull(message = "Дата окончания обязательна")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDate endDate;


}