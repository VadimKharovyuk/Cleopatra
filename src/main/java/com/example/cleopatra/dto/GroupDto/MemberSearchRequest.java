package com.example.cleopatra.dto.GroupDto;


import com.example.cleopatra.enums.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для поиска участников
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberSearchRequest {

    private String query; // поиск по имени или email
    private GroupRole role; // фильтр по роли
    private String sortBy; // сортировка
    private String sortOrder;
    private Integer page;
    private Integer size;
}
