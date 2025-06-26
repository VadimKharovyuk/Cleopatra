package com.example.cleopatra.service.impl;

import com.example.cleopatra.dto.GroupDto.*;
import com.example.cleopatra.enums.GroupRole;
import com.example.cleopatra.enums.MembershipStatus;
import com.example.cleopatra.model.GroupMembership;
import com.example.cleopatra.model.Group;
import com.example.cleopatra.model.User;
import com.example.cleopatra.repository.GroupMembershipRepository;
import com.example.cleopatra.repository.GroupRepository;
import com.example.cleopatra.repository.UserRepository;
import com.example.cleopatra.ExistsException.GroupAccessDeniedException;
import com.example.cleopatra.ExistsException.GroupNotFoundException;

import com.example.cleopatra.service.GroupMembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupMembershipServiceImpl implements GroupMembershipService {

    private final GroupMembershipRepository membershipRepository;
    private final GroupRepository groupRepository;


    /**
     * Получить настройки группы со всеми участниками
     */
    @Transactional(readOnly = true)
    public GroupSettingsDto getGroupSettings(Long groupId, Long currentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Группа не найдена"));

        // Проверяем права доступа
        GroupMembership currentUserMembership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, currentUserId, MembershipStatus.APPROVED)
                .orElseThrow(() -> new GroupAccessDeniedException("Недостаточно прав для просмотра настроек"));

        if (!currentUserMembership.hasAdminRights()) {
            throw new GroupAccessDeniedException("Только администраторы могут управлять группой");
        }

        // Получаем всех участников
        List<GroupMembership> allMemberships = membershipRepository.findByGroupIdWithUsers(groupId);

        // Разделяем по статусам
        List<GroupMembershipDto> members = allMemberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                .map(m -> mapToMembershipDto(m, currentUserMembership))
                .collect(Collectors.toList());

        List<PendingMembershipDto> pendingMemberships = allMemberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.PENDING)
                .map(this::mapToPendingDto)
                .collect(Collectors.toList());

        List<GroupMembershipDto> bannedMembers = allMemberships.stream()
                .filter(m -> m.getStatus() == MembershipStatus.BANNED)
                .map(m -> mapToMembershipDto(m, currentUserMembership))
                .collect(Collectors.toList());

        return GroupSettingsDto.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .groupDescription(group.getDescription())
                .totalMembers((long) members.size())
                .pendingRequests((long) pendingMemberships.size())
                .bannedMembersCount((long) bannedMembers.size()) // Обновлено имя поля
                .members(members)
                .pendingMemberships(pendingMemberships)
                .bannedMembersList(bannedMembers) // Обновлено имя поля
                .canManageMembers(currentUserMembership.hasAdminRights())
                .canBanMembers(currentUserMembership.hasAdminRights())
                .canChangeRoles(currentUserMembership.isOwner())
                .canDeleteGroup(currentUserMembership.isOwner())
                .build();
    }

    /**
     * Одобрить заявку на вступление
     */
    public MembershipActionResponse approveMembership(Long membershipId, Long currentUserId) {
        GroupMembership membership = getMembershipById(membershipId);
        validateAdminRights(membership.getGroupId(), currentUserId);

        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new IllegalStateException("Заявка уже обработана");
        }

        membership.setStatus(MembershipStatus.APPROVED);
        membership.setJoinedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        log.info("Заявка {} одобрена пользователем {}", membershipId, currentUserId);

        return MembershipActionResponse.builder()
                .success(true)
                .message("Заявка успешно одобрена")
                .action("approve")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Отклонить заявку на вступление
     */
    public MembershipActionResponse rejectMembership(Long membershipId, Long currentUserId, String reason) {
        GroupMembership membership = getMembershipById(membershipId);
        validateAdminRights(membership.getGroupId(), currentUserId);

        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new IllegalStateException("Заявка уже обработана");
        }

        membership.setStatus(MembershipStatus.REJECTED);
        membershipRepository.save(membership);

        log.info("Заявка {} отклонена пользователем {} по причине: {}", membershipId, currentUserId, reason);

        return MembershipActionResponse.builder()
                .success(true)
                .message("Заявка отклонена")
                .action("reject")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Заблокировать участника
     */
    public MembershipActionResponse banMember(BanMemberRequest request, Long currentUserId) {
        GroupMembership membership = getMembershipById(request.getMembershipId());
        validateAdminRights(membership.getGroupId(), currentUserId);

        // Нельзя заблокировать владельца
        if (membership.getRole() == GroupRole.OWNER) {
            throw new IllegalStateException("Нельзя заблокировать владельца группы");
        }

        // Получаем информацию о текущем пользователе
        GroupMembership currentUserMembership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(membership.getGroupId(), currentUserId, MembershipStatus.APPROVED)
                .orElseThrow(() -> new GroupAccessDeniedException("Недостаточно прав"));

        // Администратор не может заблокировать другого администратора (только владелец может)
        if (membership.getRole() == GroupRole.ADMIN && !currentUserMembership.isOwner()) {
            throw new IllegalStateException("Только владелец может заблокировать администратора");
        }

        membership.setStatus(MembershipStatus.BANNED);
        membership.setBannedAt(LocalDateTime.now());
        membership.setBanReason(request.getReason());
        membership.setBannedByUserId(currentUserId);
        membershipRepository.save(membership);

        log.info("Участник {} заблокирован в группе {} пользователем {} по причине: {}",
                membership.getUserId(), membership.getGroupId(), currentUserId, request.getReason());

        return MembershipActionResponse.builder()
                .success(true)
                .message("Участник заблокирован")
                .action("ban")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Разблокировать участника
     */
    public MembershipActionResponse unbanMember(Long membershipId, Long currentUserId) {
        GroupMembership membership = getMembershipById(membershipId);
        validateAdminRights(membership.getGroupId(), currentUserId);

        if (membership.getStatus() != MembershipStatus.BANNED) {
            throw new IllegalStateException("Участник не заблокирован");
        }

        membership.setStatus(MembershipStatus.APPROVED);
        membership.setBannedAt(null);
        membership.setBanReason(null);
        membership.setBannedByUserId(null);
        membershipRepository.save(membership);

        log.info("Участник {} разблокирован в группе {} пользователем {}",
                membership.getUserId(), membership.getGroupId(), currentUserId);

        return MembershipActionResponse.builder()
                .success(true)
                .message("Участник разблокирован")
                .action("unban")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Изменить роль участника
     */
    public MembershipActionResponse changeRole(ChangeRoleRequest request, Long currentUserId) {
        GroupMembership membership = getMembershipById(request.getMembershipId());

        // Только владелец может изменять роли
        GroupMembership currentUserMembership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(membership.getGroupId(), currentUserId, MembershipStatus.APPROVED)
                .orElseThrow(() -> new GroupAccessDeniedException("Недостаточно прав"));

        if (!currentUserMembership.isOwner()) {
            throw new GroupAccessDeniedException("Только владелец может изменять роли");
        }

        // Нельзя изменить роль владельца
        if (membership.getRole() == GroupRole.OWNER) {
            throw new IllegalStateException("Нельзя изменить роль владельца");
        }

        // Нельзя назначить нового владельца
        if (request.getNewRole() == GroupRole.OWNER) {
            throw new IllegalStateException("Нельзя назначить нового владельца через эту функцию");
        }

        GroupRole oldRole = membership.getRole();
        membership.setRole(request.getNewRole());
        membershipRepository.save(membership);

        log.info("Роль участника {} в группе {} изменена с {} на {} пользователем {}. Причина: {}",
                membership.getUserId(), membership.getGroupId(), oldRole, request.getNewRole(),
                currentUserId, request.getReason());

        return MembershipActionResponse.builder()
                .success(true)
                .message("Роль участника изменена")
                .action("change_role")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Исключить участника из группы
     */
    public MembershipActionResponse removeMember(Long membershipId, Long currentUserId, String reason) {
        GroupMembership membership = getMembershipById(membershipId);
        validateAdminRights(membership.getGroupId(), currentUserId);

        // Нельзя исключить владельца
        if (membership.getRole() == GroupRole.OWNER) {
            throw new IllegalStateException("Нельзя исключить владельца группы");
        }

        membership.setStatus(MembershipStatus.LEFT);
        membership.setLeftAt(LocalDateTime.now());
        membershipRepository.save(membership);

        log.info("Участник {} исключен из группы {} пользователем {} по причине: {}",
                membership.getUserId(), membership.getGroupId(), currentUserId, reason);

        return MembershipActionResponse.builder()
                .success(true)
                .message("Участник исключен из группы")
                .action("remove")
                .timestamp(LocalDateTime.now())
                .build();
    }
    /**
     * Получить список всех участников группы
     */
    @Override
    @Transactional(readOnly = true)
    public GroupMembersListDto getAllGroupMembers(Long groupId, Long currentUserId) {
        return getAllGroupMembers(groupId, currentUserId, null, null, null, 0, 50);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMembersListDto getAllGroupMembers(Long groupId, Long currentUserId,
                                                  String roleFilter, String sortBy, String sortOrder,
                                                  Integer page, Integer size) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Группа не найдена"));

        // Проверяем права доступа - участник группы может видеть список
        GroupMembership currentUserMembership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, currentUserId, MembershipStatus.APPROVED)
                .orElseThrow(() -> new GroupAccessDeniedException("Только участники группы могут просматривать список участников"));

        // Получаем всех одобренных участников
        List<GroupMembership> allMembers = membershipRepository.findByGroupIdAndStatusWithUserOrderByJoinedAtDesc(
                groupId, MembershipStatus.APPROVED);

        // Применяем фильтры
        List<GroupMembership> filteredMembers = allMembers.stream()
                .filter(member -> {
                    if (roleFilter == null || "ALL".equals(roleFilter)) {
                        return true;
                    }
                    return member.getRole().name().equals(roleFilter);
                })
                .collect(Collectors.toList());

        // Применяем сортировку
        if (sortBy != null) {
            Comparator<GroupMembership> comparator = getComparator(sortBy);
            if ("desc".equals(sortOrder)) {
                comparator = comparator.reversed();
            }
            filteredMembers.sort(comparator);
        }

        // Применяем пагинацию
        int totalElements = filteredMembers.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);

        List<GroupMembership> pagedMembers = filteredMembers.subList(startIndex, endIndex);

        // Преобразуем в DTO
        List<GroupMembershipDto> memberDtos = pagedMembers.stream()
                .map(m -> mapToMembershipDto(m, currentUserMembership))
                .collect(Collectors.toList());

        return GroupMembersListDto.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .totalMembers((long) totalElements)
                .members(memberDtos)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .roleFilter(roleFilter)
                .page(page)
                .size(size)
                .hasNext(endIndex < totalElements)
                .hasPrevious(page > 0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMembersListDto searchGroupMembers(Long groupId, Long currentUserId, MemberSearchRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Группа не найдена"));

        // Проверяем права доступа
        GroupMembership currentUserMembership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, currentUserId, MembershipStatus.APPROVED)
                .orElseThrow(() -> new GroupAccessDeniedException("Только участники группы могут просматривать список участников"));

        // Получаем всех одобренных участников
        List<GroupMembership> allMembers = membershipRepository.findByGroupIdAndStatusWithUserOrderByJoinedAtDesc(
                groupId, MembershipStatus.APPROVED);

        // Применяем поиск и фильтры
        List<GroupMembership> filteredMembers = allMembers.stream()
                .filter(member -> {
                    // Фильтр по запросу (имя или email)
                    if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
                        String query = request.getQuery().toLowerCase();
                        String fullName = (member.getUser().getFirstName() + " " + member.getUser().getLastName()).toLowerCase();
                        String email = member.getUser().getEmail().toLowerCase();

                        if (!fullName.contains(query) && !email.contains(query)) {
                            return false;
                        }
                    }

                    // Фильтр по роли
                    if (request.getRole() != null) {
                        return member.getRole() == request.getRole();
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Применяем сортировку
        if (request.getSortBy() != null) {
            Comparator<GroupMembership> comparator = getComparator(request.getSortBy());
            if ("desc".equals(request.getSortOrder())) {
                comparator = comparator.reversed();
            }
            filteredMembers.sort(comparator);
        }

        // Применяем пагинацию
        int totalElements = filteredMembers.size();
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);

        List<GroupMembership> pagedMembers = filteredMembers.subList(startIndex, endIndex);

        // Преобразуем в DTO
        List<GroupMembershipDto> memberDtos = pagedMembers.stream()
                .map(m -> mapToMembershipDto(m, currentUserMembership))
                .collect(Collectors.toList());

        return GroupMembersListDto.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .totalMembers((long) totalElements)
                .members(memberDtos)
                .sortBy(request.getSortBy())
                .sortOrder(request.getSortOrder())
                .page(page)
                .size(size)
                .hasNext(endIndex < totalElements)
                .hasPrevious(page > 0)
                .build();
    }

    @Override
    /**
     * Получить краткий список участников (для автодополнения, упоминаний и т.д.)
     */
    @Transactional(readOnly = true)
    public List<GroupMemberSummaryDto> getGroupMembersSummary(Long groupId, Long currentUserId) {
        // Проверяем права доступа
        membershipRepository.findByGroupIdAndUserIdAndStatus(groupId, currentUserId, MembershipStatus.APPROVED)
                .orElseThrow(() -> new GroupAccessDeniedException("Только участники группы могут просматривать список участников"));

        List<GroupMembership> members = membershipRepository.findByGroupIdAndStatusWithUserOrderByJoinedAtDesc(
                groupId, MembershipStatus.APPROVED);

        return members.stream()
                .map(this::mapToMemberSummaryDto)
                .collect(Collectors.toList());
    }


    // ==================== HELPER METHODS ====================

    // ==================== HELPER METHODS ====================

    private Comparator<GroupMembership> getComparator(String sortBy) {
        return switch (sortBy) {
            case "name" -> Comparator.comparing(m -> m.getUser().getFirstName() + " " + m.getUser().getLastName());
            case "joinDate" -> Comparator.comparing(GroupMembership::getJoinedAt);
            case "role" -> Comparator.comparing(m -> m.getRole().ordinal());
            case "email" -> Comparator.comparing(m -> m.getUser().getEmail());
            default -> Comparator.comparing(GroupMembership::getJoinedAt);
        };
    }

    private GroupMemberSummaryDto mapToMemberSummaryDto(GroupMembership membership) {
        User user = membership.getUser();
        return GroupMemberSummaryDto.builder()
                .membershipId(membership.getId())
                .userId(membership.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .role(membership.getRole())
                .joinedAt(membership.getJoinedAt())
                .isOnline(false) // TODO: реализовать статус онлайн
                .lastSeen(null) // TODO: реализовать время последнего посещения
                .build();
    }

    private GroupMembership getMembershipById(Long membershipId) {
        return membershipRepository.findByIdWithUser(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Участие в группе не найдено"));
    }

    private void validateAdminRights(Long groupId, Long currentUserId) {
        GroupMembership currentUserMembership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, currentUserId, MembershipStatus.APPROVED)
                .orElseThrow(() -> new GroupAccessDeniedException("Недостаточно прав"));

        if (!currentUserMembership.hasAdminRights()) {
            throw new GroupAccessDeniedException("Только администраторы могут выполнять это действие");
        }
    }

    private GroupMembershipDto mapToMembershipDto(GroupMembership membership, GroupMembership currentUserMembership) {
        User user = membership.getUser();
        boolean isCurrentUserOwner = currentUserMembership.isOwner();
        boolean isCurrentUserAdmin = currentUserMembership.hasAdminRights();
        boolean isTargetOwner = membership.getRole() == GroupRole.OWNER;
        boolean isTargetAdmin = membership.getRole() == GroupRole.ADMIN;

        return GroupMembershipDto.builder()
                .id(membership.getId())
                .userId(membership.getUserId())
                .groupId(membership.getGroupId())
                .userFirstName(user.getFirstName())
                .userLastName(user.getLastName())
                .userEmail(user.getEmail())
                .userImageUrl(user.getImageUrl())
                .role(membership.getRole())
                .status(membership.getStatus())
                .requestedAt(membership.getRequestedAt())
                .joinedAt(membership.getJoinedAt())
                .leftAt(membership.getLeftAt())
                .bannedAt(membership.getBannedAt())
                .banReason(membership.getBanReason())
                .bannedByUserId(membership.getBannedByUserId())
                .createdAt(membership.getCreatedAt())
                .updatedAt(membership.getUpdatedAt())
                // Права действий
                .canPromote(isCurrentUserOwner && !isTargetOwner && membership.getRole() != GroupRole.ADMIN)
                .canDemote(isCurrentUserOwner && !isTargetOwner && membership.getRole() != GroupRole.MEMBER)
                .canBan(isCurrentUserAdmin && !isTargetOwner && (!isTargetAdmin || isCurrentUserOwner))
                .canUnban(isCurrentUserAdmin && membership.getStatus() == MembershipStatus.BANNED)
                .canRemove(isCurrentUserAdmin && !isTargetOwner && (!isTargetAdmin || isCurrentUserOwner))
                .build();
    }

    private PendingMembershipDto mapToPendingDto(GroupMembership membership) {
        User user = membership.getUser();
        return PendingMembershipDto.builder()
                .membershipId(membership.getId())
                .userId(membership.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .requestedAt(membership.getRequestedAt())
                .bio(user.getStatusPage())
                .mutualFriendsCount(0) // TODO: реализовать подсчет общих друзей
                .isFollowedByCurrentUser(false) // TODO: реализовать проверку подписки
                .build();
    }
}