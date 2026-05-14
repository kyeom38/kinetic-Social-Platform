package com.example.demo.service;

import com.example.demo.dto.CommentDto;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /** トップレベルコメント（返信付き）を返す */
    public List<CommentDto> getByPostId(Long postId) {
        return commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId)
                .stream()
                .map(c -> {
                    CommentDto dto = toDto(c);
                    List<CommentDto> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(c.getId())
                            .stream().map(this::toDto).collect(Collectors.toList());
                    dto.setReplies(replies);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void add(Long postId, String content, Long userId, Long parentId) {
        Post post = postRepository.findById(postId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setParentId(parentId);
        commentRepository.save(comment);
    }

    /** ソフト削除: 本人または管理者のみ */
    public boolean softDelete(Long commentId, Long currentUserId, boolean isAdmin) {
        return commentRepository.findById(commentId).map(c -> {
            if (!isAdmin && (c.getUser() == null || !c.getUser().getId().equals(currentUserId))) {
                return false;
            }
            c.setDeleted(true);
            c.setContent("(削除されたコメント)");
            commentRepository.save(c);
            return true;
        }).orElse(false);
    }

    private CommentDto toDto(Comment c) {
        CommentDto dto = new CommentDto();
        dto.setId(c.getId());
        dto.setPostId(c.getPost().getId());
        dto.setParentId(c.getParentId());
        dto.setDeleted(c.isDeleted());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setContent(c.isDeleted() ? "(削除されたコメント)" : c.getContent());
        if (c.getUser() != null) {
            dto.setAuthorId(c.getUser().getId());
            String name = "ADMIN".equals(c.getUser().getRole()) ? "管理者" : c.getUser().getName();
            dto.setAuthorName(name != null ? name : "匿名");
            dto.setAuthorInitial(dto.getAuthorName().substring(0, 1));
            dto.setAuthorProfileImageUrl(c.getUser().getProfileImageUrl());
        } else {
            dto.setAuthorName("匿名");
            dto.setAuthorInitial("?");
        }
        return dto;
    }
}
