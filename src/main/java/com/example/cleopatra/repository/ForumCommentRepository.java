package com.example.cleopatra.repository;
import com.example.cleopatra.model.ForumComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {


    @Query("SELECT fc FROM ForumComment fc " +
            "JOIN FETCH fc.author " +
            "WHERE fc.id = :commentId")
    Optional<ForumComment> findByIdWithAuthor(@Param("commentId") Long commentId);

    @Query("SELECT fc FROM ForumComment fc " +
            "JOIN FETCH fc.author " +
            "WHERE fc.forum.id = :forumId AND fc.deleted = false " +
            "ORDER BY fc.createdAt ASC")
    Slice<ForumComment> findByForumIdWithAuthors(@Param("forumId") Long forumId, Pageable pageable);

    @Query("SELECT fc FROM ForumComment fc " +
            "JOIN FETCH fc.author " +
            "WHERE fc.parent.id = :parentId AND fc.deleted = false " +
            "ORDER BY fc.createdAt ASC")
    List<ForumComment> findRepliesWithAuthors(@Param("parentId") Long parentId);

    @Modifying
    @Query("UPDATE Forum f SET f.commentCount = GREATEST(0, f.commentCount - 1) WHERE f.id = :forumId")
    void decrementCommentCount(@Param("forumId") Long forumId);

    @Modifying
    @Query("UPDATE ForumComment fc SET fc.childrenCount = GREATEST(0, fc.childrenCount - 1) WHERE fc.id = :commentId")
    void decrementChildrenCount(@Param("commentId") Long commentId);


    @Modifying
    @Transactional
    void deleteByForumId(Long forumId);
}