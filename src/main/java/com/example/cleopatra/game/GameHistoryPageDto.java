package com.example.cleopatra.game;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Страница с историей игр")
public class GameHistoryPageDto {

    @Schema(description = "Список игр")
    private List<GameHistoryDto> games;

    @Schema(description = "Общее количество игр")
    private Long totalElements;

    @Schema(description = "Номер текущей страницы")
    private Integer currentPage;

    @Schema(description = "Размер страницы")
    private Integer pageSize;

    @Schema(description = "Общее количество страниц")
    private Integer totalPages;

    @Schema(description = "Есть ли следующая страница")
    private Boolean hasNext;

    @Schema(description = "Есть ли предыдущая страница")
    private Boolean hasPrevious;

    @Schema(description = "Краткая статистика")
    private PlayerGameStatsDto summary;
}
