package com.example.cleopatra.dto.AdvertisementDTO;

import com.example.cleopatra.model.Advertisement;
import lombok.Data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementList {
    private List<AdvertisementListDTO> advertisementListDTOS;

    private int currentPage;
    private int itemsPerPage;

    // Для Page
    private Integer totalPages;
    private Long totalItems;

    // Для Slice и общего удобства
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Integer nextPage;
    private Integer previousPage;

    // Конструктор из Spring Page
    public static AdvertisementList fromPage(Page<Advertisement> page) {
        List<AdvertisementListDTO> dtos = page.getContent().stream()
                .map(AdvertisementListDTO::fromEntity)
                .toList();

        return AdvertisementList.builder()
                .advertisementListDTOS(dtos)
                .currentPage(page.getNumber())
                .itemsPerPage(page.getSize())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .nextPage(page.hasNext() ? page.getNumber() + 1 : null)
                .previousPage(page.hasPrevious() ? page.getNumber() - 1 : null)
                .build();
    }

    // Конструктор из Spring Slice (когда общее количество неизвестно)
    public static AdvertisementList fromSlice(Slice<Advertisement> slice) {
        List<AdvertisementListDTO> dtos = slice.getContent().stream()
                .map(AdvertisementListDTO::fromEntity)
                .toList();

        return AdvertisementList.builder()
                .advertisementListDTOS(dtos)
                .currentPage(slice.getNumber())
                .itemsPerPage(slice.getSize())
                .totalPages(null) // Slice не знает общее количество
                .totalItems(null)
                .hasNext(slice.hasNext())
                .hasPrevious(slice.hasPrevious())
                .nextPage(slice.hasNext() ? slice.getNumber() + 1 : null)
                .previousPage(slice.hasPrevious() ? slice.getNumber() - 1 : null)
                .build();
    }
}
