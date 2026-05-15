package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CommentDto {
    private Long id;
    private Long postId;
    private Long parentId;
    private String content;
    private String authorName;
    private String authorInitial;
    private String authorProfileImageUrl;
    private Long authorId;
    private LocalDateTime createdAt;
    private boolean deleted;
    private List<CommentDto> replies;
}
