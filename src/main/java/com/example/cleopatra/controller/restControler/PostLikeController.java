package com.example.cleopatra.controller.restControler;

import com.example.cleopatra.dto.Post.PostLikeInfoDto;
import com.example.cleopatra.dto.Post.PostLikeResponseDto;
import com.example.cleopatra.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostService postService;

    @PostMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponseDto> toggleLike(@PathVariable Long postId) {
        PostLikeResponseDto response = postService.toggleLike(postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}/likes")
    public ResponseEntity<PostLikeInfoDto> getLikeInfo(@PathVariable Long postId) {
        PostLikeInfoDto response = postService.getLikeInfo(postId);
        return ResponseEntity.ok(response);
    }
}
