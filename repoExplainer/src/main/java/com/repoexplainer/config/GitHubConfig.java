package com.repoexplainer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GitHubConfig {

    @Value("${github.token:}")
    private String githubToken;
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
