package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabasePatch {

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    @Value("${app.db.reset:false}")
    private boolean dbReset;

    private static final List<String> REQUIRED_NOT_NULL = List.of("id", "created_at");

    @PostConstruct
    public void applyPatches() {

        if (dbReset) {
            if (environment.matchesProfiles("prod")) {
                log.warn("=== DB RESET: 本番環境 (prod) では実行されません ===");
            } else {
                log.warn("=== DB RESET: 全テーブルを初期化します ===");
                runSilently("TRUNCATE TABLE event_votes, comments, posts, user_hobbies, users RESTART IDENTITY CASCADE");
                log.warn("=== DB RESET 完了 ===");
            }
        }

        // ── posts: NOT NULL 制約を落とす ──────────────────────────────
        List<String> nonNullCols = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'posts' AND is_nullable = 'NO'",
                String.class
        );
        for (String col : nonNullCols) {
            if (REQUIRED_NOT_NULL.contains(col)) continue;
            runSilently("ALTER TABLE posts ALTER COLUMN \"" + col + "\" DROP NOT NULL");
        }

        // ── posts: 新カラム追加 ────────────────────────────────────────
        runSilently("ALTER TABLE posts ALTER COLUMN cover_image_url TYPE TEXT");
        runSilently("ALTER TABLE posts ALTER COLUMN source_url TYPE TEXT");
        runSilently("ALTER TABLE posts ADD COLUMN IF NOT EXISTS event_date DATE");
        runSilently("ALTER TABLE posts ADD COLUMN IF NOT EXISTS capacity INTEGER");
        runSilently("ALTER TABLE posts ADD COLUMN IF NOT EXISTS location VARCHAR(255)");
        runSilently("ALTER TABLE posts ADD COLUMN IF NOT EXISTS announcement BOOLEAN NOT NULL DEFAULT FALSE");
        runSilently("ALTER TABLE posts ADD COLUMN IF NOT EXISTS view_count INTEGER NOT NULL DEFAULT 0");
        runSilently("ALTER TABLE posts ADD COLUMN IF NOT EXISTS sub_category VARCHAR(50)");
        runSilently("ALTER TABLE posts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");

        // ── users: 新カラム追加 ────────────────────────────────────────
        runSilently("ALTER TABLE users ADD COLUMN IF NOT EXISTS department VARCHAR(255)");
        runSilently("ALTER TABLE users ADD COLUMN IF NOT EXISTS job_title VARCHAR(255)");
        runSilently("ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE");
        runSilently("ALTER TABLE users ADD COLUMN IF NOT EXISTS retired_at DATE");
        runSilently("ALTER TABLE users ADD COLUMN IF NOT EXISTS mbti VARCHAR(10)");

        // ── users: manager boolean → job_title へ移行（旧DB互換）──────
        runSilently(
            "UPDATE users SET job_title = CASE WHEN manager = true THEN '部長' ELSE '一般社員' END " +
            "WHERE job_title IS NULL AND manager IS NOT NULL"
        );

        // ── comments: スレッド返信 + ソフト削除 ──────────────────────
        runSilently("ALTER TABLE comments ADD COLUMN IF NOT EXISTS parent_id BIGINT REFERENCES comments(id)");
        runSilently("ALTER TABLE comments ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE");

        // ── event_votes: attending を nullable に（未定=NULL を許可）──
        runSilently("ALTER TABLE event_votes ALTER COLUMN attending DROP NOT NULL");

        // ── users: 初回ログイン強制パスワード変更フラグ ──────────────
        runSilently("ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE");

        // ── users: アカウント作成日（組織図 NEW バッジ用）────────────
        runSilently("ALTER TABLE users ADD COLUMN IF NOT EXISTS joined_at DATE");
        // 既存ユーザーには今日の日付を仮設定（NULLのまま残すと NEW バッジが出ないので問題ない）

        // ── データ品質: 空文字列をNULLに統一 ──────────────────────────
        runSilently("UPDATE posts SET sub_category = NULL WHERE sub_category = ''");
        runSilently("UPDATE posts SET source_url = NULL WHERE source_url = ''");
    }

    private void runSilently(String sql) {
        try {
            jdbcTemplate.execute(sql);
            log.info("DB patch: {}", sql);
        } catch (Exception e) {
            // 既に適用済み or 対象外は無視
        }
    }
}
