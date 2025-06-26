package com.example.cleopatra.repository;

import com.example.cleopatra.dto.GroupDto.GroupPostDetails;
import com.example.cleopatra.model.GroupPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {

    /**
     * Получение постов группы без информации о лайках пользователя
     */
    @Query("""
        SELECT new com.example.cleopatra.dto.GroupDto.GroupPostDetails(
            gp.id,
            gp.text,
            gp.imageUrl,
            gp.createdAt,
            gp.authorId,
            u.firstName,
            u.imageUrl,
            gp.likeCount,
            gp.commentCount,
            CASE WHEN gp.imageUrl IS NOT NULL THEN true ELSE false END,
            false
        )
        FROM GroupPost gp
        JOIN gp.author u
        WHERE gp.groupId = :groupId
        ORDER BY gp.createdAt DESC
    """)
    Slice<GroupPostDetails> findGroupPostsByGroupId(
            @Param("groupId") Long groupId,
            Pageable pageable
    );

    /**
     * Пока лайков нет, используем тот же запрос что и выше
     * Параметр currentUserId игнорируется, но оставляем для совместимости
     */
    @Query("""
        SELECT new com.example.cleopatra.dto.GroupDto.GroupPostDetails(
            gp.id,
            gp.text,
            gp.imageUrl,
            gp.createdAt,
            gp.authorId,
            u.firstName,
            u.imageUrl,
            gp.likeCount,
            gp.commentCount,
            CASE WHEN gp.imageUrl IS NOT NULL THEN true ELSE false END,
            false
        )
        FROM GroupPost gp
        JOIN gp.author u
        WHERE gp.groupId = :groupId
        ORDER BY gp.createdAt DESC
    """)
    Slice<GroupPostDetails> findGroupPostsByGroupIdWithLikes(
            @Param("groupId") Long groupId,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );


}