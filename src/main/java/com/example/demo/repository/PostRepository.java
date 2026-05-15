package com.example.demo.repository;

import com.example.demo.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCategoryOrderByCreatedAtDesc(String category);
    List<Post> findAllByOrderByCreatedAtDesc();
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Post> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);
    Page<Post> findByCategoryAndSubCategoryOrderByCreatedAtDesc(String category, String subCategory, Pageable pageable);
    List<Post> findByAnnouncementTrueOrderByCreatedAtDesc();
    Optional<Post> findTopByCategoryOrderByViewCountDesc(String category);
    Optional<Post> findTopByOrderByViewCountDesc();

    // イベント日が指定日以降
    List<Post> findByCategoryInAndEventDateGreaterThanEqualOrderByEventDateAsc(List<String> categories, LocalDate date);

    // 締め切り日が指定日以降かつeventDateなし（募集中ポスト）
    List<Post> findByDeadlineGreaterThanEqualAndEventDateIsNullOrderByDeadlineAsc(LocalDate date);

    // 趣味・おしゃべりのサブカテゴリ絞り込み
    List<Post> findByCategoryAndSubCategoryOrderByCreatedAtDesc(String category, String subCategory);

    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%',:kw,'%')) ORDER BY p.createdAt DESC")
    Page<Post> searchByKeyword(@Param("kw") String keyword, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.category = :cat AND (LOWER(p.title) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%',:kw,'%'))) ORDER BY p.createdAt DESC")
    Page<Post> searchByCategoryAndKeyword(@Param("cat") String category, @Param("kw") String keyword, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.createdAt = :ts WHERE p.id = :id")
    void updateCreatedAt(@Param("id") Long id, @Param("ts") java.time.LocalDateTime ts);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.category = :newName WHERE p.category = :oldName")
    int renameCategory(@Param("oldName") String oldName, @Param("newName") String newName);
}
