package com.example.demo.controller;

import com.example.demo.service.PostService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import PostDto;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PostService postService;
    private final UserService userService;

    @GetMapping("/home")
    public String home(Model model) {
        List<PostDto> announcements = postService.getAnnouncements();
        Set<Long> usedIds = new HashSet<>();
        announcements.forEach(p -> usedIds.add(p.getId()));

        PostDto latestPost = postService.getAll().stream()
                .filter(p -> !usedIds.contains(p.getId()))
                .findFirst().orElse(null);
        if (latestPost != null) usedIds.add(latestPost.getId());

        PostDto popularPost = postService.getMostViewed()
                .filter(p -> !usedIds.contains(p.getId()))
                .orElse(null);
        if (popularPost != null) usedIds.add(popularPost.getId());

        PostDto itNews = postService.getMostViewedByCategory("IT関連ニュース")
                .filter(p -> !usedIds.contains(p.getId()))
                .orElse(null);

        model.addAttribute("announcements", announcements);
        model.addAttribute("latestPost", latestPost);
        model.addAttribute("popularPost", popularPost);
        model.addAttribute("itNews", itNews);
        model.addAttribute("upcomingEvents", postService.getUpcomingEvents());
        model.addAttribute("orgChart", userService.getOrgChart());
        model.addAttribute("sevenDaysAgo", LocalDate.now().minusDays(7));
        return "home";
    }
}
