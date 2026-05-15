package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.name:管理者}")
    private String adminName;

    @Bean
    public ApplicationRunner initAdmin() {
        return args -> {
            // ADMIN ロールのユーザーが1人もいない場合のみ作成する。
            // 既存の管理者が1人でもいれば何もしない。
            // 注意: DB から全管理者を削除すると次回起動時に再作成される。
            if (!userRepository.existsByRole("ADMIN")) {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                admin.setName(adminName);
                admin.setActive(true);
                userRepository.save(admin);
                log.info("管理者アカウントを作成しました: {}", adminEmail);
            }
        };
    }
}
