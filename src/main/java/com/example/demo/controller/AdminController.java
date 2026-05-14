package com.example.demo.controller;

import com.example.demo.dto.UserDto;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;

    private static final List<String> DEPARTMENTS = List.of(
            "エンジニアリング部", "デザイン部", "マーケティング部",
            "営業部", "総務・人事部", "経営企画部"
    );
    private static final List<String> JOB_TITLES = List.of(
            "部長", "課長", "主任", "一般社員", "インターン"
    );

    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("users", userService.getActiveUsers());
        model.addAttribute("retiredUsers", userService.getRetiredUsers());
        return "admin-users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new UserDto());
        model.addAttribute("departments", DEPARTMENTS);
        model.addAttribute("jobTitles", JOB_TITLES);
        return "admin-user-new";
    }

    @PostMapping("/users")
    public String createUser(@ModelAttribute UserDto dto, RedirectAttributes ra) {
        if (dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail())) {
            ra.addFlashAttribute("createError", "そのメールアドレスはすでに登録されています: " + dto.getEmail());
            return "redirect:/admin/users/new";
        }
        if (dto.getEmployeeId() != null && !dto.getEmployeeId().isEmpty()
                && userRepository.existsByEmployeeId(dto.getEmployeeId())) {
            ra.addFlashAttribute("createError", "その社員番号はすでに登録されています: " + dto.getEmployeeId());
            return "redirect:/admin/users/new";
        }
        userService.createWithMustChangePassword(dto);
        ra.addFlashAttribute("createSuccess", "アカウントを作成しました: " + dto.getEmail());
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getById(id));
        model.addAttribute("departments", DEPARTMENTS);
        model.addAttribute("jobTitles", JOB_TITLES);
        return "admin-user-edit";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, @ModelAttribute UserDto dto, RedirectAttributes ra) {
        userService.adminUpdate(id, dto);
        ra.addFlashAttribute("saveSuccess", "保存しました");
        return "redirect:/admin/users/" + id + "/edit";
    }

    /** 管理者によるパスワードリセット */
    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                RedirectAttributes ra) {
        userService.resetPassword(id, newPassword);
        ra.addFlashAttribute("resetSuccess", "パスワードをリセットしました");
        return "redirect:/admin/users/" + id + "/edit";
    }

    /** 退職処理: 退職日を指定してソフトデリート */
    @PostMapping("/users/{id}/delete")
    public String retireUser(@PathVariable Long id,
                             @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate retiredAt,
                             RedirectAttributes ra) {
        userService.retire(id, retiredAt);
        return "redirect:/admin/users";
    }
}
