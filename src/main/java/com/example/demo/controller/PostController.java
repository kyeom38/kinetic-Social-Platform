package com.example.demo.controller;

import com.example.demo.dto.PostDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CommentService;
import com.example.demo.service.NewsScheduler;
import com.example.demo.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final NewsScheduler newsScheduler;
    private final UserRepository userRepository;
    private final CommentService commentService;

    private static final List<String> ALL_CATEGORIES = List.of(
            "社内ニュース・イベント", "IT関連ニュース", "お誘い", "その他のイベント", "趣味・おしゃべり"
    );
    private static final List<String> ADMIN_ONLY_CATEGORIES = List.of("社内ニュース・イベント");
    private static final List<String> HOBBY_SUBS = List.of(
            "スポーツ", "音楽", "アニメ", "旅行", "ゲーム", "動物", "その他"
    );
    private static final List<String> EVENT_REQUIRED_CATEGORIES = List.of("お誘い", "その他のイベント");
    private static final List<String> EVENT_OPTIONAL_CATEGORIES = List.of("社内ニュース・イベント");
    private static final List<String> NEWS_CATEGORIES = List.of("IT関連ニュース");
    private static final List<String> HOBBY_CATEGORIES = List.of("趣味・おしゃべり");

    private static final Map<String, String> CATEGORY_DESCRIPTIONS = Map.of(
        "社内ニュース・イベント", "📢 社内公式のお知らせや行事の告知が掲載されます。重要な情報はホーム画面にも表示されます。",
        "IT関連ニュース",        "💡 気になる外部記事やニュースをみんなと共有する場所です。参照URLが設定された記事は外部サイトに直接飛べます。",
        "お誘い",               "🍺 飲み会・ゴルフ・ボードゲーム・ライブ観戦・展示会・勉強会など、一緒に楽しめることなら何でも！参加・不参加・未定を投票できます。",
        "その他のイベント",      "📍 地域のお祭り・割引イベント・展示会など、社員の生活に役立つ情報から告知まで。参加投票もできます。",
        "趣味・おしゃべり",     "🎨 趣味の話題を気軽に投稿できます。おすすめ作品・旅行記録・スポーツ観戦の感想など何でもOK。サブカテゴリで絞り込みできます。"
    );

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(required = false) String subCategory,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        org.springframework.data.domain.Page<PostDto> postsPage;
        if (hasKeyword) {
            postsPage = (category != null && !category.isEmpty())
                    ? postService.searchByCategoryPaged(category, keyword, page)
                    : postService.searchPaged(keyword, page);
        } else {
            postsPage = (category != null && !category.isEmpty() && subCategory != null && !subCategory.isEmpty())
                    ? postService.getByCategoryAndSubCategoryPaged(category, subCategory, page)
                    : (category != null && !category.isEmpty())
                            ? postService.getByCategoryPaged(category, page)
                            : postService.getAllPaged(page);
        }
        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("selectedCategory", category != null ? category : "");
        model.addAttribute("selectedSubCategory", subCategory != null ? subCategory : "");
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("categories", ALL_CATEGORIES);
        model.addAttribute("hobbySubs", HOBBY_SUBS);
        model.addAttribute("today", LocalDate.now());
        if (category != null && !category.isEmpty()) {
            model.addAttribute("categoryDescription", CATEGORY_DESCRIPTIONS.get(category));
        }
        return "bulletin-board";
    }

    @GetMapping("/new")
    public String createForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<String> categories = isAdmin(userDetails) ? ALL_CATEGORIES
                : ALL_CATEGORIES.stream().filter(c -> !ADMIN_ONLY_CATEGORIES.contains(c)).collect(Collectors.toList());
        model.addAttribute("post", new PostDto());
        model.addAttribute("categories", categories);
        model.addAttribute("eventRequiredCategories", EVENT_REQUIRED_CATEGORIES);
        model.addAttribute("eventOptionalCategories", EVENT_OPTIONAL_CATEGORIES);
        model.addAttribute("newsCategories", NEWS_CATEGORIES);
        model.addAttribute("hobbyCategories", HOBBY_CATEGORIES);
        return "post-create";
    }

    @PostMapping
    public String create(@ModelAttribute PostDto dto,
                         @AuthenticationPrincipal UserDetails userDetails) {
        boolean admin = isAdmin(userDetails);
        // フロントエンドで選択肢を制限しているため通常は発生しない。
        // 万が一非管理者が管理者専用カテゴリで送信した場合は無音で投稿フォームに戻す。
        if (!admin && ADMIN_ONLY_CATEGORIES.contains(dto.getCategory())) {
            return "redirect:/posts/new";
        }
        User user = userRepository.findFirstByEmail(userDetails.getUsername()).orElse(null);
        if (user != null) {
            dto.setAuthorId(user.getId());
        }
        PostDto created = postService.create(dto);
        return "redirect:/posts/" + created.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findFirstByEmail(userDetails.getUsername()).orElse(null);
        Long userId = user != null ? user.getId() : null;
        model.addAttribute("post", postService.getById(id, userId));
        model.addAttribute("comments", commentService.getByPostId(id));
        model.addAttribute("currentUserId", userId);
        model.addAttribute("isAdmin", isAdmin(userDetails));
        return "post-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        PostDto post = postService.getById(id, null);
        User user = userRepository.findFirstByEmail(userDetails.getUsername()).orElse(null);
        boolean admin = isAdmin(userDetails);
        if (!admin && (user == null || !user.getId().equals(post.getAuthorId()))) {
            return "redirect:/posts/" + id;
        }
        List<String> categories = admin ? ALL_CATEGORIES
                : ALL_CATEGORIES.stream().filter(c -> !ADMIN_ONLY_CATEGORIES.contains(c)).collect(Collectors.toList());
        model.addAttribute("post", post);
        model.addAttribute("categories", categories);
        model.addAttribute("eventRequiredCategories", EVENT_REQUIRED_CATEGORIES);
        model.addAttribute("eventOptionalCategories", EVENT_OPTIONAL_CATEGORIES);
        model.addAttribute("newsCategories", NEWS_CATEGORIES);
        model.addAttribute("hobbyCategories", HOBBY_CATEGORIES);
        return "post-edit";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute PostDto dto,
                       @AuthenticationPrincipal UserDetails userDetails) {
        PostDto existing = postService.getById(id, null);
        User user = userRepository.findFirstByEmail(userDetails.getUsername()).orElse(null);
        boolean admin = isAdmin(userDetails);
        if (!admin && (user == null || !user.getId().equals(existing.getAuthorId()))) {
            return "redirect:/posts/" + id;
        }
        if (!admin && ADMIN_ONLY_CATEGORIES.contains(dto.getCategory())) {
            dto.setCategory(existing.getCategory());
        }
        postService.update(id, dto);
        return "redirect:/posts/" + id;
    }

    @PostMapping("/fetch-news")
    public String fetchNews(@AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return "redirect:/posts";
        newsScheduler.fetchAndPostNews();
        return "redirect:/posts?category=IT関連ニュース";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String category,
                         @AuthenticationPrincipal UserDetails userDetails,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        boolean admin = isAdmin(userDetails);
        User user = userRepository.findFirstByEmail(userDetails.getUsername()).orElse(null);
        boolean deleted = postService.deleteIfAuthorized(id, user != null ? user.getId() : null, admin);
        if (deleted) ra.addFlashAttribute("deleteSuccess", "投稿を削除しました");
        return category != null && !category.isEmpty()
                ? "redirect:/posts?category=" + category
                : "redirect:/posts";
    }
}
