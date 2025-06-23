package com.example.cleopatra.service;

import com.example.cleopatra.dto.Forum.ForumCreateDTO;
import com.example.cleopatra.dto.Forum.ForumCreateResponseDTO;
import com.example.cleopatra.dto.Forum.ForumDetailDTO;
import com.example.cleopatra.dto.Forum.ForumPageResponseDTO;
import com.example.cleopatra.enums.ForumType;

public interface ForumService {


    ForumCreateResponseDTO createForum(ForumCreateDTO forumCreateDTO, String userEmail);


    void deleteForum(Long forumId, String userEmail, boolean isAdmin);

    ForumDetailDTO viewForum(Long forumId, String userEmail);



}
