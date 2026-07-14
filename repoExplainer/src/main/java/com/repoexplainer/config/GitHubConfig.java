package com.repoexplainer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * GitHub API için WebClient bean tanımı.
 *
 * Ne yapar:
 * - GitHub API'ye istek atacak bir HTTP client oluşturur.
 * - Her istekte otomatik olarak token ve accept header'ı ekler.
 * - Token ortam değişkeninden okunur (GITHUB_TOKEN).
 */
@Configuration
public class GitHubConfig {

    @Value("${github.token:}")
    private String githubToken;

    /**
     * GitHub API istekleri için hazır bir WebClient döndürür.
     * Token varsa "Authorization: Bearer <token>" header'ı eklenir.
     * Token yoksa da çalışır ama saatte 60 istek sınırı olur.
     */
    @Bean
    public WebClient githubWebClient() {
        WebClient.Builder builder = WebClient.builder().baseUrl("https://api.github.com").defaultHeader("Accept",
                "application/vnd.github+json");

        if (githubToken != null && !githubToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + githubToken);
        }

        return builder.build();
    }
}
