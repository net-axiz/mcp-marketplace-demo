package com.repoexplainer.github;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Supported formats: https://github.com/owner/repo, owner/repo, with optional .git suffix
public class GitHubUrlParser {
    public record GitHubRepo(String owner, String repo) {}

    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?(?:github\\.com/)?([^/]+)/([^/]+?)(?:\\.git)?/?$"
    );
    public static Optional<GitHubRepo> parse(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = GITHUB_PATTERN.matcher(url.trim());

        if (matcher.matches()) {
            String owner = matcher.group(1);
            String repo = matcher.group(2);
            return Optional.of(new GitHubRepo(owner, repo));
        }

        return Optional.empty();
    }
}
