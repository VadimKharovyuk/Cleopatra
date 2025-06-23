package com.example.cleopatra.service;

import com.example.cleopatra.dto.ForumComment.CreateForumCommentDto;
import com.example.cleopatra.dto.ForumComment.ForumCommentDto;
import com.example.cleopatra.dto.ForumComment.ForumCommentPageDto;

import java.util.List;

public interface ForumCommentService {
    ForumCommentDto createForumComment(CreateForumCommentDto forumCommentDto, Long userId);
    boolean deleteForumComment(Long forumCommentId, Long userId);
    ForumCommentPageDto getForumComments(Long forumId, int page, int size);
    List<ForumCommentDto> getCommentReplies(Long parentCommentId);

    List<ForumCommentDto> getAllCommentReplies(Long commentId);
}
