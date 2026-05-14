package com.example.demo.repository;

import com.example.demo.entity.EventVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<EventVote, Long> {
    int countByPostIdAndAttending(Long postId, Boolean attending);
    Optional<EventVote> findByPostIdAndUserId(Long postId, Long userId);
    List<EventVote> findByPostIdAndAttending(Long postId, Boolean attending);

    @Query("SELECT COUNT(v) FROM EventVote v WHERE v.post.id = :postId AND v.attending IS NULL")
    int countUndecidedByPostId(@Param("postId") Long postId);

    @Query("SELECT v FROM EventVote v WHERE v.post.id = :postId AND v.attending IS NULL")
    List<EventVote> findUndecidedByPostId(@Param("postId") Long postId);
}
