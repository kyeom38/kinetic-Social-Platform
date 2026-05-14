package com.example.demo.service;

import com.example.demo.entity.Post;
import com.example.demo.repository.PostRepository;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final PostRepository postRepository;

    private static final List<String> RSS_FEEDS = List.of(
            "https://zenn.dev/topics/ai/feed",
            "https://zenn.dev/topics/programming/feed",
            "https://qiita.com/popular-items/feed.atom"
    );

    // 毎朝8時に実行
    @Scheduled(cron = "0 0 8 * * *")
    public void fetchAndPostNews() {
        log.info("IT関連ニュース自動取得開始");
        for (String feedUrl : RSS_FEEDS) {
            try {
                SyndFeed feed = new SyndFeedInput().build(new XmlReader(URI.create(feedUrl).toURL().openStream()));
                for (SyndEntry entry : feed.getEntries()) {
                    String title = entry.getTitle();
                    String link = entry.getLink();
                    String rawDescription = entry.getDescription() != null
                            ? entry.getDescription().getValue() : "";

                    // 同じタイトルが既に存在する場合はスキップ
                    boolean exists = postRepository.findAllByOrderByCreatedAtDesc()
                            .stream().anyMatch(p -> title.equals(p.getTitle()));
                    if (exists) continue;

                    // サマリー: HTMLタグを除去して最初の200文字
                    String summary = rawDescription.replaceAll("<[^>]+>", "").trim();
                    if (summary.length() > 200) summary = summary.substring(0, 200) + "…";

                    // 画像URL: enclosure → media module → description内のimg タグ の順で取得
                    String imageUrl = null;
                    if (!entry.getEnclosures().isEmpty()) {
                        SyndEnclosure enc = entry.getEnclosures().get(0);
                        if (enc.getUrl() != null && !enc.getUrl().isEmpty()) {
                            imageUrl = enc.getUrl();
                        }
                    }
                    if (imageUrl == null) {
                        Matcher m = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']").matcher(rawDescription);
                        if (m.find()) imageUrl = m.group(1);
                    }

                    Post post = new Post();
                    post.setTitle(title);
                    post.setContent(summary);
                    post.setSourceUrl(link);
                    post.setCoverImageUrl(imageUrl);
                    post.setCategory("IT関連ニュース");
                    postRepository.save(post);
                }
            } catch (Exception e) {
                log.error("RSS取得エラー: {}", feedUrl, e);
            }
        }
        log.info("IT関連ニュース自動取得完了");
    }
}
