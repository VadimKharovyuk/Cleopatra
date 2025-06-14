package com.example.cleopatra.dto.BirthdayUser;

import lombok.Data;

import java.util.List;

@Data
public class BirthdayPageResponse {
    // Данные
    private List<BirthdayUserCardDto> users;

    // Метаданные пагинации
    private Boolean hasNext;
    private Integer currentPage;
    private Integer size;
    private Boolean isEmpty;
    private Integer numberOfElements;

    // Дополнительные метаданные для дней рождения
    private String period;        // "today", "tomorrow", "week", "month"
    private Integer totalBirthdays; // общее количество именинников
}
