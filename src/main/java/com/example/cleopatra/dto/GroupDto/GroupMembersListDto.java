package com.example.cleopatra.dto.GroupDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO для списка всех участников группы
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMembersListDto {

    private Long groupId;
    private String groupName;
    private Long totalMembers;
    private List<GroupMembershipDto> members;

    // Фильтры
    private String sortBy; // "name", "joinDate", "role"
    private String sortOrder; // "asc", "desc"
    private String roleFilter; // "ALL", "OWNER", "ADMIN", "MODERATOR", "MEMBER"

    // Пагинация
    private Integer page;
    private Integer size;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
