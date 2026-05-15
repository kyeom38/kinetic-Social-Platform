package com.example.demo.service;

import com.example.demo.dto.PostDto;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;

    private static final int PAGE_SIZE = 12;

    public List<PostDto> getAll() {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toListDto).collect(Collectors.toList());
    }

    public List<PostDto> getByCategory(String category) {
        return postRepository.findByCategoryOrderByCreatedAtDesc(category)
                .stream().map(this::toListDto).collect(Collectors.toList());
    }

    public List<PostDto> getByCategoryAndSubCategory(String category, String subCategory) {
        return postRepository.findByCategoryAndSubCategoryOrderByCreatedAtDesc(category, subCategory)
                .stream().map(this::toListDto).collect(Collectors.toList());
    }

    public Page<PostDto> getAllPaged(int page) {
        return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, PAGE_SIZE))
                .map(this::toListDto);
    }

    public Page<PostDto> getByCategoryPaged(String category, int page) {
        return postRepository.findByCategoryOrderByCreatedAtDesc(category, PageRequest.of(page, PAGE_SIZE))
                .map(this::toListDto);
    }

    public Page<PostDto> getByCategoryAndSubCategoryPaged(String category, String subCategory, int page) {
        return postRepository.findByCategoryAndSubCategoryOrderByCreatedAtDesc(category, subCategory, PageRequest.of(page, PAGE_SIZE))
                .map(this::toListDto);
    }

    public Page<PostDto> searchPaged(String keyword, int page) {
        return postRepository.searchByKeyword(keyword, PageRequest.of(page, PAGE_SIZE))
                .map(this::toListDto);
    }

    public Page<PostDto> searchByCategoryPaged(String category, String keyword, int page) {
        return postRepository.searchByCategoryAndKeyword(category, keyword, PageRequest.of(page, PAGE_SIZE))
                .map(this::toListDto);
    }

    public PostDto getById(Long id, Long currentUserId) {
        Post post = postRepository.findById(id).orElseThrow(() -> {
            log.warn("Post not found: id={}", id);
            return new RuntimeException("Post not found: " + id);
        });
        postRepository.incrementViewCount(id);
        return toDto(post, currentUserId);
    }

    public void delete(Long id) {
        postRepository.deleteById(id);
    }

    public boolean deleteIfAuthorized(Long id, Long currentUserId, boolean isAdmin) {
        return postRepository.findById(id).map(p -> {
            Long authorId = p.getAuthor() != null ? p.getAuthor().getId() : null;
            boolean authorized = isAdmin ||
                    (currentUserId != null && authorId != null && currentUserId.equals(authorId));
            if (!authorized) {
                log.warn("Unauthorized delete attempt: postId={}, userId={}", id, currentUserId);
                return false;
            }
            postRepository.deleteById(id);
            return true;
        }).orElse(false);
    }

    public PostDto update(Long id, PostDto dto) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found: " + id));
        post.setTitle(dto.getTitle() != null ? dto.getTitle() : "");
        post.setContent(dto.getContent() != null ? dto.getContent() : "");
        post.setCategory(dto.getCategory() != null ? dto.getCategory() : post.getCategory());
        post.setDeadline(dto.getDeadline());
        post.setEventDate(dto.getEventDate());
        post.setCapacity(dto.getCapacity());
        post.setLocation(blankToNull(dto.getLocation()));
        post.setSubCategory(blankToNull(dto.getSubCategory()));
        post.setSourceUrl(blankToNull(dto.getSourceUrl()));
        if (dto.getCoverImageUrl() != null && !dto.getCoverImageUrl().isBlank()) {
            post.setCoverImageUrl(dto.getCoverImageUrl());
        }
        return toDto(postRepository.save(post), null);
    }

    public PostDto create(PostDto dto) {
        Post post = new Post();
        post.setTitle(dto.getTitle() != null ? dto.getTitle() : "");
        post.setContent(dto.getContent() != null ? dto.getContent() : "");
        post.setCategory(dto.getCategory() != null ? dto.getCategory() : "");
        post.setDeadline(dto.getDeadline());
        post.setEventDate(dto.getEventDate());
        post.setCapacity(dto.getCapacity());
        post.setSubCategory(blankToNull(dto.getSubCategory()));
        post.setAnnouncement(dto.isAnnouncement());
        post.setSourceUrl(blankToNull(dto.getSourceUrl()));
        post.setCoverImageUrl(blankToNull(dto.getCoverImageUrl()));
        post.setLocation(blankToNull(dto.getLocation()));
        if (dto.getAuthorId() != null) {
            User author = userRepository.findById(dto.getAuthorId()).orElse(null);
            post.setAuthor(author);
        }
        return toDto(postRepository.save(post), null);
    }

    public List<PostDto> getAnnouncements() {
        return postRepository.findByAnnouncementTrueOrderByCreatedAtDesc()
                .stream().map(this::toListDto).collect(Collectors.toList());
    }

    public Optional<PostDto> getMostViewed() {
        return postRepository.findTopByOrderByViewCountDesc().map(this::toListDto);
    }

    public Optional<PostDto> getMostViewedByCategory(String category) {
        return postRepository.findTopByCategoryOrderByViewCountDesc(category).map(this::toListDto);
    }

    /**
     * ホーム画面のイベント欄: eventDate >= 今日 のイベント投稿 +
     * deadline >= 今日 かつ eventDate なし の募集中ポストも表示する
     */
    public List<PostDto> getUpcomingEvents() {
        LocalDate today = LocalDate.now();
        List<String> eventCategories = List.of("お誘い", "その他のイベント", "社内ニュース・イベント");

        // イベント日が設定された投稿
        List<PostDto> byEvent = postRepository
                .findByCategoryInAndEventDateGreaterThanEqualOrderByEventDateAsc(eventCategories, today)
                .stream().map(this::toListDto).collect(Collectors.toList());

        // 募集締め切りのみ設定された投稿（eventDateなし）
        List<PostDto> byDeadline = postRepository
                .findByDeadlineGreaterThanEqualAndEventDateIsNullOrderByDeadlineAsc(today)
                .stream().map(this::toListDto).collect(Collectors.toList());

        List<PostDto> result = new ArrayList<>(byEvent);
        result.addAll(byDeadline);
        return result;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // 一覧用: vote / comment リポジトリを呼ばない。
    // 1ページ12件を一括取得する際に呼ぶと N+1 クエリになるため、
    // カード表示に不要なデータ（参加者名・コメント数など）はここでは取得しない。
    private PostDto toListDto(Post post) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setCategory(post.getCategory());
        dto.setCoverImageUrl(post.getCoverImageUrl());
        dto.setSourceUrl(post.getSourceUrl());
        dto.setLocation(post.getLocation());
        dto.setSubCategory(post.getSubCategory());
        dto.setDeadline(post.getDeadline());
        dto.setEventDate(post.getEventDate());
        dto.setCapacity(post.getCapacity());
        dto.setAnnouncement(post.isAnnouncement());
        dto.setViewCount(post.getViewCount());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        if (post.getAuthor() != null) {
            dto.setAuthorId(post.getAuthor().getId());
            dto.setAuthorName("ADMIN".equals(post.getAuthor().getRole()) ? "管理者" : post.getAuthor().getName());
            dto.setAuthorProfileImageUrl(post.getAuthor().getProfileImageUrl());
        }
        return dto;
    }

    // 詳細用: コメント数・投票数・参加者名などを全件取得する。getById / create / update でのみ使用。
    private PostDto toDto(Post post, Long currentUserId) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setCategory(post.getCategory());
        dto.setCoverImageUrl(post.getCoverImageUrl());
        dto.setSourceUrl(post.getSourceUrl());
        dto.setLocation(post.getLocation());
        dto.setSubCategory(post.getSubCategory());
        dto.setDeadline(post.getDeadline());
        dto.setEventDate(post.getEventDate());
        dto.setCapacity(post.getCapacity());
        dto.setAnnouncement(post.isAnnouncement());
        dto.setViewCount(post.getViewCount());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setCommentCount(commentRepository.countByPostId(post.getId()));
        dto.setAttendingCount(voteRepository.countByPostIdAndAttending(post.getId(), Boolean.TRUE));
        dto.setNotAttendingCount(voteRepository.countByPostIdAndAttending(post.getId(), Boolean.FALSE));
        dto.setAttendeeNames(voteRepository.findByPostIdAndAttending(post.getId(), Boolean.TRUE)
                .stream().map(v -> v.getUser() != null ? v.getUser().getName() : "?").collect(java.util.stream.Collectors.toList()));
        dto.setAbsenteeNames(voteRepository.findByPostIdAndAttending(post.getId(), Boolean.FALSE)
                .stream().map(v -> v.getUser() != null ? v.getUser().getName() : "?").collect(java.util.stream.Collectors.toList()));
        dto.setUndecidedCount(voteRepository.countUndecidedByPostId(post.getId()));
        dto.setUndecidedNames(voteRepository.findUndecidedByPostId(post.getId())
                .stream().map(v -> v.getUser() != null ? v.getUser().getName() : "?")
                .collect(java.util.stream.Collectors.toList()));
        if (currentUserId != null) {
            voteRepository.findByPostIdAndUserId(post.getId(), currentUserId).ifPresent(v -> {
                if (v.getAttending() == null) {
                    dto.setMyVoteUndecided(true);
                } else {
                    dto.setMyVote(v.getAttending());
                }
            });
        }
        if (post.getAuthor() != null) {
            dto.setAuthorId(post.getAuthor().getId());
            dto.setAuthorName("ADMIN".equals(post.getAuthor().getRole()) ? "管理者" : post.getAuthor().getName());
            dto.setAuthorProfileImageUrl(post.getAuthor().getProfileImageUrl());
        }
        return dto;
    }
}
