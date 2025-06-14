package com.example.cleopatra.service;

import com.example.cleopatra.dto.BlockedUse.BlockedUserDto;
import com.example.cleopatra.dto.BlockedUse.BlockedUsersPageResponse;
import com.example.cleopatra.dto.BlockedUse.UserBlockResponse;
import com.example.cleopatra.dto.user.UserResponse;
import com.example.cleopatra.model.User;
import com.example.cleopatra.model.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;



public interface UserBlockService {

    /**
     * Заблокировать пользователя
     */
    UserBlockResponse blockUser(Long blockerId, Long blockedId);

    /**
     * Разблокировать пользователя
     */
    void unblockUser(Long blockerId, Long blockedId);

    /**
     * Получить заблокированных пользователей с пагинацией
     */
    Page<UserBlock> getBlockedUsersPage(User blocker, Pageable pageable);

    /**
     * Получить заблокированных пользователей как DTO с пагинацией
     */
    BlockedUsersPageResponse getBlockedUsersPageResponse(User blocker, Pageable pageable);

    /**
     * Получить список заблокированных пользователей как DTO
     */
    List<BlockedUserDto> getBlockedUsersDto(User blocker);


    long getBlockedUsersCount(Long id);


    boolean isBlocked(Long id, Long userId);


    long getTotalBlockedUsersCount();
}