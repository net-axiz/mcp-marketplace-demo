package com.repoexplainer.github;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Desteklenen formatlar:
 * - https://github.com/owner/repo
 * - http://github.com/owner/repo
 * - www.github.com/owner/repo
 * - github.com/owner/repo
 * - owner/repo
 * - https://github.com/owner/repo.git
 * - https://github.com/owner/repo/
 */
public class GitHubUrlParser {
    public record GitHubRepo(String owner, String repo) {}

    /*
     * Regex açıklaması:
     * ^(?:https?://)?   → İsteğe bağlı http:// veya https://
     * (?:www\.)?        → İsteğe bağlı www.
     * (?:github\.com/)? → İsteğe bağlı github.com/
     * ([^/]+)           → 1. Grup = Owner (/ haricindeki karakterler)
     * /                 → Araya giren /
     * ([^/]+?)          → 2. Grup = Repo (tembel eşleştirme)
     * (?:\.git)?        → İsteğe bağlı .git uzantısı
     * /?$               → İsteğe bağlı sondaki / ve satır sonu
     */
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
