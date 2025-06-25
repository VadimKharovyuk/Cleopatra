package com.example.cleopatra.service;

import com.example.cleopatra.dto.GroupDto.*;
import com.example.cleopatra.enums.GroupRole;
import org.springframework.data.domain.Pageable;

public interface GroupService {

    GroupResponseDTO createGroup(CreateGroupRequestDTO createGroupRequestDTO ,Long ownerId);

    GroupResponseDTO getGroupById(Long groupId, Long currentUserId);
//
    GroupPageResponse getAllPublicGroups(Pageable pageable);           // публичные + закрытые
    GroupPageResponse getAllAvailableGroups(Pageable pageable, Long currentUserId); // + приватные где участник
//    GroupPageResponse getAllGroupsForAdmin(Pageable pageable);         // для админов системы
//
    void deleteGroup(Long groupId, Long currentUserId);
//    GroupResponseDTO updateGroup(Long groupId, UpdateGroupRequestDTO dto, Long currentUserId);

}
