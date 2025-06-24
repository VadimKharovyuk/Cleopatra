package com.example.cleopatra.repository;

import com.example.cleopatra.enums.GroupStatus;
import com.example.cleopatra.enums.GroupType;
import com.example.cleopatra.model.Group;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {




    Slice<Group> findByGroupTypeInAndGroupStatus(
            List<GroupType> groupTypes,
            GroupStatus groupStatus,
            Pageable pageable
    );


    @Query("""
        SELECT DISTINCT g FROM Group g 
        LEFT JOIN g.memberships m 
        WHERE g.groupStatus = :status 
        AND (
            g.groupType IN ('OPEN', 'CLOSED') 
            OR (g.groupType = 'PRIVATE' AND m.userId = :userId AND m.status = 'APPROVED')
        )
        ORDER BY g.createdAt DESC
        """)
    Slice<Group> findAvailableGroupsForUser(
            @Param("userId") Long userId,
            @Param("status") GroupStatus status,
            Pageable pageable
    );
}
