package com.example.cleopatra.maper;

import com.example.cleopatra.dto.GroupDto.CreateGroupRequestDTO;
import com.example.cleopatra.dto.GroupDto.GroupResponseDTO;
import com.example.cleopatra.dto.GroupDto.UpdateGroupRequestDTO;
import com.example.cleopatra.enums.GroupRole;
import com.example.cleopatra.enums.MembershipStatus;
import com.example.cleopatra.model.Group;
import com.example.cleopatra.model.GroupMembership;
import com.example.cleopatra.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupMapper {

    /**
     * Конвертирует CreateGroupRequestDTO в Group entity
     */
    public Group toEntity(CreateGroupRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Group.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .groupType(dto.getGroupType())
                // Изображения будут установлены в сервисе после загрузки
                .imageUrl(null)
                .imgId(null)
                .backgroundImageUrl(null)
                .backgroundImgId(null)
                .build();
    }

    /**
     * Конвертирует Group entity в GroupResponseDTO
     * @param group - сущность группы
     * @param currentUserId - ID текущего пользователя (для определения статуса)
     */
    public GroupResponseDTO toResponseDTO(Group group, Long currentUserId) {
        if (group == null) {
            return null;
        }

        GroupResponseDTO.GroupResponseDTOBuilder builder = GroupResponseDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .imageUrl(group.getImageUrl())
                .imgId(group.getImgId())
                .backgroundImageUrl(group.getBackgroundImageUrl())
                .backgroundImgId(group.getBackgroundImgId())
                .groupType(group.getGroupType())
                .groupStatus(group.getGroupStatus())
                .memberCount(group.getMemberCount())
                .ownerId(group.getOwnerId())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt());

        // Добавляем информацию о владельце
        if (group.getOwner() != null) {
            User owner = group.getOwner();
            builder.ownerFirstName(owner.getFirstName())
                    .ownerLastName(owner.getLastName())
                    .ownerImageUrl(owner.getImageUrl());
        }

        // Определяем статус текущего пользователя в группе
        if (currentUserId != null) {
            GroupMembership currentUserMembership = findUserMembership(group, currentUserId);

            if (currentUserMembership != null) {
                builder.currentUserMembershipStatus(currentUserMembership.getStatus())
                        .currentUserRole(currentUserMembership.getRole())
                        .currentUserJoinedAt(currentUserMembership.getJoinedAt())
                        .isMember(currentUserMembership.isApproved());
            } else {
                builder.currentUserMembershipStatus(null)
                        .currentUserRole(null)
                        .currentUserJoinedAt(null)
                        .isMember(false);
            }

            // Устанавливаем флаги для удобства фронтенда
            setPermissionFlags(builder, group, currentUserMembership, currentUserId);
        }

        return builder.build();
    }

    /**
     * Обновляет Group entity данными из UpdateGroupRequestDTO
     */
    public void updateEntityFromDTO(Group group, UpdateGroupRequestDTO dto) {
        if (group == null || dto == null) {
            return;
        }

        if (dto.getName() != null) {
            group.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            group.setDescription(dto.getDescription());
        }
        if (dto.getGroupType() != null) {
            group.setGroupType(dto.getGroupType());
        }
        // Изображения обновляются в сервисе после загрузки
    }

    /**
     * Находит членство пользователя в группе
     */
    private GroupMembership findUserMembership(Group group, Long userId) {
        if (group.getMemberships() == null || userId == null) {
            return null;
        }

        return group.getMemberships().stream()
                .filter(membership -> Objects.equals(membership.getUserId(), userId))
                .filter(membership -> !MembershipStatus.LEFT.equals(membership.getStatus()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Устанавливает флаги разрешений для текущего пользователя
     */
    private void setPermissionFlags(GroupResponseDTO.GroupResponseDTOBuilder builder,
                                    Group group,
                                    GroupMembership membership,
                                    Long currentUserId) {

        boolean isOwner = Objects.equals(group.getOwnerId(), currentUserId);
        boolean isAdmin = membership != null && membership.hasAdminRights();
        boolean isMember = membership != null && membership.isApproved();
        boolean isPending = membership != null && membership.isPending();
        boolean isBanned = membership != null && membership.isBanned();

        // Может ли присоединиться к группе
        boolean canJoin = !isMember && !isPending && !isBanned &&
                (group.isOpen() || group.isClosed()); // В приватные нельзя без приглашения

        // Может ли покинуть группу (владелец не может покинуть)
        boolean canLeave = isMember && !isOwner;

        // Может ли управлять группой
        boolean canManage = isOwner || isAdmin;

        builder.canJoin(canJoin)
                .canLeave(canLeave)
                .canManage(canManage)
                .isMember(isMember);
    }

    /**
     * Конвертирует Group entity в упрощенный DTO (для списков)
     */
    public GroupResponseDTO toSimpleResponseDTO(Group group, Long currentUserId) {
        if (group == null) {
            return null;
        }

        return GroupResponseDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .imageUrl(group.getImageUrl())
                .groupType(group.getGroupType())
                .memberCount(group.getMemberCount())
                .ownerId(group.getOwnerId())
                .createdAt(group.getCreatedAt())
                // Минимальная информация для списков
                .isMember(currentUserId != null && isMemberOfGroup(group, currentUserId))
                .build();
    }

    /**
     * Проверяет, является ли пользователь участником группы
     */
    private boolean isMemberOfGroup(Group group, Long userId) {
        if (group.getMemberships() == null || userId == null) {
            return false;
        }

        return group.getMemberships().stream()
                .anyMatch(membership -> Objects.equals(membership.getUserId(), userId)
                        && membership.isApproved());
    }

}