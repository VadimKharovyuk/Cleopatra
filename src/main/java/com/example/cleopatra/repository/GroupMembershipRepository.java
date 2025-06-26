package com.example.cleopatra.repository;

import com.example.cleopatra.enums.MembershipStatus;
import com.example.cleopatra.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    // Продолжение GroupMembershipRepository

    Optional<GroupMembership> findByGroupIdAndUserIdAndStatus(
            Long groupId, Long userId, MembershipStatus status);

    List<GroupMembership> findByGroupIdAndStatus(Long groupId, MembershipStatus status);

    @Query("SELECT COUNT(gm) FROM GroupMembership gm " +
            "WHERE gm.groupId = :groupId AND gm.status = :status")
    Long countByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("status") MembershipStatus status);

    @Query("SELECT gm FROM GroupMembership gm " +
            "LEFT JOIN FETCH gm.user u " +
            "LEFT JOIN FETCH gm.bannedByUser " +
            "WHERE gm.groupId = :groupId AND gm.status = :status " +
            "ORDER BY gm.requestedAt DESC")
    List<GroupMembership> findByGroupIdAndStatusWithUserOrderByRequestedAtDesc(
            @Param("groupId") Long groupId, @Param("status") MembershipStatus status);

    @Query("SELECT gm FROM GroupMembership gm " +
            "LEFT JOIN FETCH gm.user " +
            "WHERE gm.groupId = :groupId")
    List<GroupMembership> findByGroupIdWithUsers(@Param("groupId") Long groupId);

    @Query("SELECT gm FROM GroupMembership gm " +
            "LEFT JOIN FETCH gm.user " +
            "WHERE gm.id = :id")
    Optional<GroupMembership> findByIdWithUser(@Param("id") Long id);


// Добавить в GroupMembershipRepository

    @Query("SELECT gm FROM GroupMembership gm " +
            "LEFT JOIN FETCH gm.user " +
            "WHERE gm.groupId = :groupId AND gm.status = :status " +
            "ORDER BY gm.joinedAt DESC")
    List<GroupMembership> findByGroupIdAndStatusWithUserOrderByJoinedAtDesc(
            @Param("groupId") Long groupId, @Param("status") MembershipStatus status);
}

