package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class PostDto {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String coverImageUrl;
    private String sourceUrl;
    private String location;
    private String subCategory;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventDate;
    private Integer capacity;
    private boolean announcement;
    private int viewCount;
    private Long authorId;
    private String authorName;
    private String authorProfileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int commentCount;
    private int attendingCount;
    private int notAttendingCount;
    private Boolean myVote;        // null=未投票, true=参加, false=不参加
    private boolean myVoteUndecided; // true=未定投票済み
    private int undecidedCount;
    private List<String> attendeeNames;
    private List<String> absenteeNames;
    private List<String> undecidedNames;
}
