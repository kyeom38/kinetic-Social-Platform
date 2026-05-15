package com.example.demo.config;

import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全コントローラーのモデルに "currentUser"（User エンティティ）を自動注入する。
 * これにより各コントローラーで個別に取得する必要がなく、
 * すべての Thymeleaf テンプレートで ${currentUser} が使える。
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserRepository userRepository;

    @ModelAttribute
    public void addCurrentUser(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            userRepository.findFirstByEmail(userDetails.getUsername())
                    .ifPresent(u -> model.addAttribute("currentUser", u));
        }
    }
}
