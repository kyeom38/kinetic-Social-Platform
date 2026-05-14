package com.example.demo.controller;

import com.example.demo.service.PostService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PostService postService;
    private final UserService userService;

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("announcements", postService.getAnnouncements());
        model.addAttribute("latestPost", postService.getAll().stream().findFirst().orElse(null));
        model.addAttribute("popularPost", postService.getMostViewed().orElse(null));
        model.addAttribute("itNews", postService.getMostViewedByCategory("IT関連ニュース").orElse(null));
        model.addAttribute("upcomingEvents", postService.getUpcomingEvents());
        model.addAttribute("orgChart", userService.getOrgChart());
        model.addAttribute("sevenDaysAgo", LocalDate.now().minusDays(7));
        return "home";
    }
}
