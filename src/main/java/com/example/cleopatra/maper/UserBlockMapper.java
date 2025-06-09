package com.example.cleopatra.maper;

import com.example.cleopatra.dto.BlockedUse.BlockedUserDto;
import com.example.cleopatra.dto.BlockedUse.BlockedUsersPageResponse;
import com.example.cleopatra.dto.BlockedUse.UserBlockResponse;
import com.example.cleopatra.model.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserBlockMapper {

    /**
     * Маппинг UserBlock в минимальный UserBlockResponse
     */
    public UserBlockResponse toUserBlockResponse(UserBlock userBlock) {
        if (userBlock == null) {
            return null;
        }

        return UserBlockResponse.builder()
                .id(userBlock.getId())
                .blockerId(userBlock.getBlocker().getId())
                .blockedId(userBlock.getBlocked().getId())
                .blockedAt(userBlock.getBlockedAt())
                .build();
    }

    /**
     * Маппинг UserBlock в BlockedUserDto (с информацией о заблокированном пользователе)
     */
    public BlockedUserDto toBlockedUserDto(UserBlock userBlock) {
        if (userBlock == null) {
            return null;
        }

        var blockedUser = userBlock.getBlocked();

        return BlockedUserDto.builder()
                .blockId(userBlock.getId())
                .blockedAt(userBlock.getBlockedAt())
                .userId(blockedUser.getId())
                .firstName(blockedUser.getFirstName())
                .lastName(blockedUser.getLastName())
                .email(blockedUser.getEmail())
                .imageUrl(blockedUser.getImageUrl())
                .city(blockedUser.getCity())
                .isOnline(blockedUser.getIsOnline())
                .lastSeen(blockedUser.getLastSeen())
                .followersCount(blockedUser.getFollowersCount())
                .followingCount(blockedUser.getFollowingCount())
                .build();
    }

    /**
     * Маппинг списка UserBlock в список BlockedUserDto
     */
    public List<BlockedUserDto> toBlockedUserDtoList(List<UserBlock> userBlocks) {
        return userBlocks.stream()
                .map(this::toBlockedUserDto)
                .collect(Collectors.toList());
    }

    /**
     * Маппинг Page<UserBlock> в BlockedUsersPageResponse
     */
    public BlockedUsersPageResponse toBlockedUsersPageResponse(Page<UserBlock> page, long totalBlockedCount) {
        List<BlockedUserDto> blockedUsers = page.getContent().stream()
                .map(this::toBlockedUserDto)
                .collect(Collectors.toList());

        return BlockedUsersPageResponse.builder()
                .blockedUsers(blockedUsers)
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .blockedUsersCount(totalBlockedCount)
                .build();
    }
}