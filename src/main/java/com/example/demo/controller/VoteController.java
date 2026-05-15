package com.example.demo.controller;

import com.example.demo.entity.EventVote;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class VoteController {

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /** voteType: "attending"=参加, "absent"=不参加, "undecided"=未定 */
    @PostMapping("/posts/{postId}/vote")
    public String vote(@PathVariable Long postId,
                       @RequestParam String voteType,
                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findFirstByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) return "redirect:/posts/" + postId;

        // ユーザーごとに1票のみ許可（DB: event_votes の (post_id, user_id) に UNIQUE 制約）。
        // 既存レコードがあれば更新、なければ新規作成（upsert）する。
        EventVote vote = voteRepository.findByPostIdAndUserId(postId, user.getId())
                .orElseGet(() -> {
                    EventVote v = new EventVote();
                    v.setPost(postRepository.findById(postId).orElseThrow());
                    v.setUser(user);
                    return v;
                });

        Boolean attending = switch (voteType) {
            case "attending" -> Boolean.TRUE;
            case "absent"    -> Boolean.FALSE;
            default          -> null; // undecided
        };
        vote.setAttending(attending);
        voteRepository.save(vote);
        return "redirect:/posts/" + postId;
    }
}
