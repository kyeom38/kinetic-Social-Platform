package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    @PostMapping("/posts/{postId}/comments")
    public String add(@PathVariable Long postId,
                      @RequestParam String content,
                      @RequestParam(required = false) Long parentId,
                      @AuthenticationPrincipal UserDetails userDetails) {
        userRepository.findFirstByEmail(userDetails.getUsername()).ifPresent(user ->
                commentService.add(postId, content, user.getId(), parentId));
        return "redirect:/posts/" + postId + "#comments";
    }

    @PostMapping("/comments/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam Long postId,
                         @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userRepository.findFirstByEmail(userDetails.getUsername()).orElse(null);
        if (currentUser != null) {
            boolean isAdmin = "ADMIN".equals(currentUser.getRole());
            commentService.softDelete(id, currentUser.getId(), isAdmin);
        }
        return "redirect:/posts/" + postId + "#comments";
    }
}
