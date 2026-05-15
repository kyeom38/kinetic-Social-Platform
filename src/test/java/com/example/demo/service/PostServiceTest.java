package com.example.demo.service;

import com.example.demo.dto.PostDto;
import com.example.demo.entity.Post;
import com.example.demo.entity.EventVote;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private VoteRepository voteRepository;

    @InjectMocks
    private PostService postService;

    private Post samplePost;

    @BeforeEach
    void setUp() {
        User author = new User();
        author.setId(1L);
        author.setName("田中 太郎");
        author.setRole("USER");

        samplePost = new Post();
        samplePost.setId(10L);
        samplePost.setTitle("テスト投稿");
        samplePost.setContent("内容です");
        samplePost.setCategory("趣味・おしゃべり");
        samplePost.setAnnouncement(false);
        samplePost.setViewCount(5);
        samplePost.setCreatedAt(LocalDateTime.now());
        samplePost.setAuthor(author);
    }

    private void stubVoteAndComment(Long postId) {
        when(commentRepository.countByPostId(postId)).thenReturn(0);
        when(voteRepository.countByPostIdAndAttending(postId, true)).thenReturn(0);
        when(voteRepository.countByPostIdAndAttending(postId, false)).thenReturn(0);
        when(voteRepository.findByPostIdAndAttending(postId, true)).thenReturn(List.of());
        when(voteRepository.findByPostIdAndAttending(postId, false)).thenReturn(List.of());
    }

    @Test
    @DisplayName("全投稿一覧が取得できる")
    void getAll_returnsAllPosts() {
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(samplePost));

        List<PostDto> result = postService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("テスト投稿");
        assertThat(result.get(0).getAuthorName()).isEqualTo("田中 太郎");
    }

    @Test
    @DisplayName("カテゴリで絞り込める")
    void getByCategory_filtersCorrectly() {
        when(postRepository.findByCategoryOrderByCreatedAtDesc("趣味・おしゃべり"))
                .thenReturn(List.of(samplePost));

        List<PostDto> result = postService.getByCategory("趣味・おしゃべり");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("趣味・おしゃべり");
    }

    @Test
    @DisplayName("IDで投稿取得するとviewCountがインクリメントされる")
    void getById_incrementsViewCount() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost));
        stubVoteAndComment(10L);

        postService.getById(10L, null);

        verify(postRepository).incrementViewCount(10L);
    }

    @Test
    @DisplayName("存在しない投稿IDで例外が発生する")
    void getById_throwsWhenNotFound() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getById(999L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Post not found: 999");
    }

    @Test
    @DisplayName("投稿が作成されてDTOで返される")
    void create_savesAndReturnsDto() {
        PostDto dto = new PostDto();
        dto.setTitle("新しい投稿");
        dto.setContent("内容");
        dto.setCategory("掲示板");
        dto.setSourceUrl("  ");  // 空白 → null に変換されるべき

        Post saved = new Post();
        saved.setId(20L);
        saved.setTitle("新しい投稿");
        saved.setContent("内容");
        saved.setCategory("掲示板");
        saved.setCreatedAt(LocalDateTime.now());

        when(postRepository.save(any(Post.class))).thenReturn(saved);
        stubVoteAndComment(20L);

        PostDto result = postService.create(dto);

        assertThat(result.getId()).isEqualTo(20L);
        // 空白のsourceUrlがnullとして保存されているか検証
        verify(postRepository).save(argThat(p -> p.getSourceUrl() == null));
    }

    @Test
    @DisplayName("投稿削除がリポジトリに委譲される")
    void delete_delegatesToRepository() {
        postService.delete(10L);
        verify(postRepository).deleteById(10L);
    }

    @Test
    @DisplayName("投稿更新で既存カテゴリが維持される")
    void update_preservesCategoryIfNotProvided() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);
        stubVoteAndComment(10L);

        PostDto dto = new PostDto();
        dto.setTitle("更新タイトル");
        dto.setContent("更新内容");
        dto.setCategory(null);  // nullの場合は既存カテゴリを維持

        postService.update(10L, dto);

        verify(postRepository).save(argThat(p -> "趣味・おしゃべり".equals(p.getCategory())));
    }

    @Test
    @DisplayName("管理者の投稿はauthorNameが「管理者」になる")
    void toDto_adminAuthorShowsAsAdmin() {
        User adminUser = new User();
        adminUser.setId(99L);
        adminUser.setName("システム管理者");
        adminUser.setRole("ADMIN");
        samplePost.setAuthor(adminUser);

        when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost));
        stubVoteAndComment(10L);

        PostDto result = postService.getById(10L, null);

        assertThat(result.getAuthorName()).isEqualTo("管理者");
    }

    @Test
    @DisplayName("参加者名がvoteから取得される")
    void getById_includesAttendeeNames() {
        User voter = new User();
        voter.setId(5L);
        voter.setName("参加者A");

        EventVote vote = new EventVote();
        vote.setUser(voter);
        vote.setAttending(true);

        when(postRepository.findById(10L)).thenReturn(Optional.of(samplePost));
        when(commentRepository.countByPostId(10L)).thenReturn(0);
        when(voteRepository.countByPostIdAndAttending(10L, true)).thenReturn(1);
        when(voteRepository.countByPostIdAndAttending(10L, false)).thenReturn(0);
        when(voteRepository.findByPostIdAndAttending(10L, true)).thenReturn(List.of(vote));
        when(voteRepository.findByPostIdAndAttending(10L, false)).thenReturn(List.of());

        PostDto result = postService.getById(10L, null);

        assertThat(result.getAttendeeNames()).containsExactly("参加者A");
        assertThat(result.getAttendingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("今後のイベントがeventDateとdeadlineの両方から取得される")
    void getUpcomingEvents_mergesBothSources() {
        Post eventPost = new Post();
        eventPost.setId(1L);
        eventPost.setTitle("飲み会");
        eventPost.setCategory("お誘い");
        eventPost.setEventDate(LocalDate.now().plusDays(7));
        eventPost.setCreatedAt(LocalDateTime.now());

        Post deadlinePost = new Post();
        deadlinePost.setId(2L);
        deadlinePost.setTitle("募集中");
        deadlinePost.setCategory("その他のイベント");
        deadlinePost.setDeadline(LocalDate.now().plusDays(3));
        deadlinePost.setCreatedAt(LocalDateTime.now());

        when(postRepository.findByCategoryInAndEventDateGreaterThanEqualOrderByEventDateAsc(any(), any()))
                .thenReturn(List.of(eventPost));
        when(postRepository.findByDeadlineGreaterThanEqualAndEventDateIsNullOrderByDeadlineAsc(any()))
                .thenReturn(List.of(deadlinePost));

        List<PostDto> result = postService.getUpcomingEvents();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("飲み会");
        assertThat(result.get(1).getTitle()).isEqualTo("募集中");
    }
}
