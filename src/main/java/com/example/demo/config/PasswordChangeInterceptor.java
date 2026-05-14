package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class PasswordChangeInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String path = request.getRequestURI();

        // パスワード変更ページ・静的リソース・ログイン関連はそのまま通す
        if (path.contains("/password") || path.startsWith("/css") || path.startsWith("/js")
                || path.startsWith("/favicon") || path.contains("/login") || path.contains("/logout")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        User user = userRepository.findFirstByEmail(auth.getName()).orElse(null);
        if (user != null && user.isMustChangePassword()) {
            response.sendRedirect("/users/" + user.getId() + "/password?forced=true");
            return false;
        }
        return true;
    }
}
