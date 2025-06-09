package com.example.cleopatra.dto.StoryDTO;

import com.example.cleopatra.dto.StoryDTO.StoryDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoryList {

    private List<StoryDTO> stories;

    // Пагинация для бесконечной прокрутки
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Integer currentPage;
    private Integer nextPage;
    private Integer previousPage;
    private Integer pageSize;

    // Дополнительная информация
    private Boolean isEmpty;
    private Integer numberOfElements;
    private Long totalElements;
    private Integer totalPages;

    // Конструктор для удобного создания из Spring Page
    public static StoryList fromPage(org.springframework.data.domain.Page<StoryDTO> page) {
        StoryList list = new StoryList();
        list.setStories(page.getContent());
        list.setHasNext(page.hasNext());
        list.setHasPrevious(page.hasPrevious());
        list.setCurrentPage(page.getNumber());
        list.setNextPage(page.hasNext() ? page.getNumber() + 1 : null);
        list.setPreviousPage(page.hasPrevious() ? page.getNumber() - 1 : null);
        list.setPageSize(page.getSize());
        list.setIsEmpty(page.isEmpty());
        list.setNumberOfElements(page.getNumberOfElements());
        list.setTotalElements(page.getTotalElements());
        list.setTotalPages(page.getTotalPages());
        return list;
    }

    // Конструктор для создания из Spring Slice (без totalElements)
    public static StoryList fromSlice(org.springframework.data.domain.Slice<StoryDTO> slice) {
        StoryList list = new StoryList();
        list.setStories(slice.getContent());
        list.setHasNext(slice.hasNext());
        list.setHasPrevious(slice.hasPrevious());
        list.setCurrentPage(slice.getNumber());
        list.setNextPage(slice.hasNext() ? slice.getNumber() + 1 : null);
        list.setPreviousPage(slice.hasPrevious() ? slice.getNumber() - 1 : null);
        list.setPageSize(slice.getSize());
        list.setIsEmpty(slice.isEmpty());
        list.setNumberOfElements(slice.getNumberOfElements());
        list.setTotalElements(null); // В Slice нет totalElements
        list.setTotalPages(null); // В Slice нет totalPages
        return list;
    }
}