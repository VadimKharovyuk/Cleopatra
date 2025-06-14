package com.example.cleopatra.dto.WallPost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WallPostPageResponse {
    private List<WallPostCardResponse> wallPosts;


    // Метаданные пагинации для Slice
    private Boolean hasNext;
    private Integer currentPage;
    private Integer size;
    private Boolean isEmpty;          // пустой ли результат
    private Integer numberOfElements; // количество элементов на текущей странице
}
