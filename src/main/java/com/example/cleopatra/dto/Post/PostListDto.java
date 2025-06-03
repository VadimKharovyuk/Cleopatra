package com.example.cleopatra.dto.Post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListDto {

    private List<PostCardDto> posts;

    private Boolean hasNext;
    private Boolean hasPrevious;
    private Integer currentPage;
    private Integer nextPage;
    private Integer previousPage;
    private Integer pageSize;

    // Дополнительная информация
    private Boolean isEmpty;
    private Integer numberOfElements;
}
