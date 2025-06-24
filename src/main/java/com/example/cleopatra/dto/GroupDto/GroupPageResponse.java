package com.example.cleopatra.dto.GroupDto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupPageResponse {

    private List<GroupResponseDTO> groups;

    // Метаданные пагинации для Slice
    private Boolean hasNext;
    private Integer currentPage;
    private Integer size;
    private Boolean isEmpty;
    private Integer numberOfElements;
}
