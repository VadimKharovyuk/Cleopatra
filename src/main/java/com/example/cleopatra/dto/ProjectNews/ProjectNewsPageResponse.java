package com.example.cleopatra.dto.ProjectNews;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectNewsPageResponse {

    private List<ProjectNewsResponse> content;

    // Метаданные для бесконечного скролла
    private Boolean hasNext;
    private Integer currentPage;
    private Integer size;
    private Boolean isEmpty;
    private Integer numberOfElements;
    private Long totalElements;
    private Integer totalPages;


}

