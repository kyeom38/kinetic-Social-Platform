package com.example.demo.service;

import com.example.demo.dto.CommentDto;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private Post post;
    private User author;
    private Comment comment;

    @BeforeEach
    void setUp() {
        post = new Post();
        post.setId(1L);

        author = new User();
        author.setId(10L);
        author.setName("テストユーザー");
        author.setRole("USER");

        comment = new Comment();
        comment.setId(100L);
        comment.setPost(post);
        comment.setUser(author);
        comment.setContent("テストコメント");
        comment.setDeleted(false);
    }

    @Test
    @DisplayName("本人はコメントをソフト削除できる")
    void softDelete_byAuthor_succeeds() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        boolean result = commentService.softDelete(100L, 10L, false);

        assertThat(result).isTrue();
        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getContent()).isEqualTo("(削除されたコメント)");
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("他人はコメントを削除できない")
    void softDelete_byOtherUser_returnsFalse() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        boolean result = commentService.softDelete(100L, 99L, false);

        assertThat(result).isFalse();
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("管理者は他人のコメントも削除できる")
    void softDelete_byAdmin_succeeds() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        boolean result = commentService.softDelete(100L, 99L, true);

        assertThat(result).isTrue();
        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("存在しないコメントIDはfalseを返す")
    void softDelete_notFound_returnsFalse() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = commentService.softDelete(999L, 10L, false);

        assertThat(result).isFalse();
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("投稿IDでトップレベルコメントと返信が取得できる")
    void getByPostId_returnsCommentsWithReplies() {
        Comment reply = new Comment();
        reply.setId(101L);
        reply.setPost(post);
        reply.setUser(author);
        reply.setContent("返信コメント");
        reply.setParentId(100L);
        reply.setDeleted(false);

        when(commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(comment));
        when(commentRepository.findByParentIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(reply));

        List<CommentDto> result = commentService.getByPostId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("テストコメント");
        assertThat(result.get(0).getReplies()).hasSize(1);
        assertThat(result.get(0).getReplies().get(0).getContent()).isEqualTo("返信コメント");
    }

    @Test
    @DisplayName("削除済みコメントは内容が「(削除されたコメント)」で表示される")
    void getByPostId_deletedComment_showsPlaceholder() {
        comment.setDeleted(true);
        comment.setContent("(削除されたコメント)");

        when(commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(comment));
        when(commentRepository.findByParentIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of());

        List<CommentDto> result = commentService.getByPostId(1L);

        assertThat(result.get(0).getContent()).isEqualTo("(削除されたコメント)");
        assertThat(result.get(0).isDeleted()).isTrue();
    }

    @Test
    @DisplayName("管理者著者のコメントはauthorNameが「管理者」になる")
    void getByPostId_adminAuthor_showsAsAdmin() {
        author.setRole("ADMIN");
        author.setName("システム管理者");

        when(commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(comment));
        when(commentRepository.findByParentIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of());

        List<CommentDto> result = commentService.getByPostId(1L);

        assertThat(result.get(0).getAuthorName()).isEqualTo("管理者");
    }
}
