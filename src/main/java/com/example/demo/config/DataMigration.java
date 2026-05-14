package com.example.demo.config;

import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigration {

    private final PostRepository postRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Order(20)
    public void migrate() {
        int renamed = postRepository.renameCategory("飲み会のお誘い", "お誘い");
        if (renamed > 0) {
            log.info("DataMigration: '飲み会のお誘い' → 'お誘い' {}件更新", renamed);
        }
    }
}
