package com.example.demo.controller;

import com.example.demo.dto.UserDto;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private boolean isAdmin(UserDetails ud) {
        return ud != null && ud.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Long id, Model model,
                          @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("user", userService.getById(id));
        if (userDetails != null) {
            UserDto current = userService.getByEmail(userDetails.getUsername());
            boolean isAdmin = isAdmin(userDetails);
            boolean isOwnProfile = current != null && id.equals(current.getId());
            model.addAttribute("isOwnProfile", isOwnProfile);
            model.addAttribute("isAdmin", isAdmin);
        } else {
            model.addAttribute("isOwnProfile", false);
            model.addAttribute("isAdmin", false);
        }
        return "profile";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        UserDto current = userService.getByEmail(userDetails.getUsername());
        if (current == null || !id.equals(current.getId())) {
            return "redirect:/users/" + id;
        }
        model.addAttribute("user", userService.getById(id));
        return "profile-edit";
    }

    /** 社員自身によるプロフィール編集（employeeId・department・jobTitleは変更不可） */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute UserDto dto,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes ra) {
        UserDto current = userService.getByEmail(userDetails.getUsername());
        if (current == null || !id.equals(current.getId())) {
            return "redirect:/users/" + id;
        }
        userService.update(id, dto);
        ra.addFlashAttribute("success", "プロフィールを保存しました");
        return "redirect:/users/" + id;
    }

    /** プロフィールアイコンを選択して保存 */
    @PostMapping("/{id}/photo")
    public String updatePhoto(@PathVariable Long id,
                              @RequestParam("icon") String icon) {
        if (icon != null && !icon.isBlank()) {
            userService.updatePhoto(id, icon);
        }
        return "redirect:/users/" + id;
    }

    /** プロフィールアイコン選択ページ */
    @GetMapping("/{id}/photo/edit")
    public String photoEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getById(id));
        model.addAttribute("iconGroups", ICON_GROUPS);
        return "photo-edit";
    }

    private static final java.util.LinkedHashMap<String, java.util.List<String>> ICON_GROUPS = new java.util.LinkedHashMap<>() {{
        put("人", java.util.List.of("👤","🧑","👩","👨","🧑‍💻","👩‍💼","👨‍💼","🧑‍🎨","🧑‍🍳","🧑‍🚀"));
        put("スポーツ", java.util.List.of("⚽","🏀","🎾","🏊","🏃","🚴","🤸","⛷️","🏋️","🎿"));
        put("ゲーム", java.util.List.of("🎮","🕹️","🎲","♟️","🎯","🃏","🀄","🎳"));
        put("動物", java.util.List.of("🐱","🐶","🐻","🦊","🐼","🦁","🐯","🐧","🐨","🐸"));
        put("お酒", java.util.List.of("🍺","🍷","🍸","🍹","🥂","🍻","🥃","🍾"));
        put("音楽・マイク", java.util.List.of("🎤","🎵","🎸","🎹","🥁","🎺","🎻","🎼"));
        put("旅行", java.util.List.of("✈️","🌍","🗺️","🏔️","🗼","🏖️","🚀","🚂","⛵","🏕️"));
        put("食べ物", java.util.List.of("🍜","🍣","🍕","🍔","🌮","🍩","☕","🧋"));
    }};

    @GetMapping("/{id}/password")
    public String passwordForm(@PathVariable Long id,
                               @RequestParam(required = false) String forced,
                               Model model) {
        model.addAttribute("userId", id);
        model.addAttribute("forced", "true".equals(forced));
        return "password-change";
    }

    @PostMapping("/{id}/password")
    public String changePassword(@PathVariable Long id,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 @RequestParam(required = false) String forced,
                                 RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "新しいパスワードが一致しません");
            return "redirect:/users/" + id + "/password" + ("true".equals(forced) ? "?forced=true" : "");
        }
        boolean ok = userService.changePassword(id, currentPassword, newPassword);
        if (!ok) {
            ra.addFlashAttribute("error", "現在のパスワードが正しくありません");
            return "redirect:/users/" + id + "/password" + ("true".equals(forced) ? "?forced=true" : "");
        }
        userService.clearMustChangePassword(id);
        ra.addFlashAttribute("success", "パスワードを変更しました");
        return "redirect:/users/" + id;
    }
}
