package com.repoexplainer.github;

import com.repoexplainer.github.model.ReadmeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class GitHubReadmeService {

    private static final Logger log = LoggerFactory.getLogger(GitHubReadmeService.class);

    private final GitHubApiClient apiClient;

    public GitHubReadmeService(GitHubApiClient apiClient) {
        this.apiClient = apiClient;
    }
    public ReadmeResult fetchReadme(String owner, String repo) {
        try {
            String content = apiClient.fetchReadme(owner, repo);

            if (content != null && !content.isBlank()) {
                log.info("README bulundu: {}/{} ({} karakter)", owner, repo, content.length());
                return new ReadmeResult.Found(content);
            } else {
                log.info("README bulunamadı: {}/{}", owner, repo);
                return new ReadmeResult.NotFound();
            }

        } catch (WebClientResponseException.Unauthorized e) {
            log.error("401 — Geçersiz token: {}/{}", owner, repo);
            return new ReadmeResult.Error("Geçersiz GitHub token (401)");

        } catch (WebClientResponseException.Forbidden e) {
            log.error("403 — Rate limit veya erişim engeli: {}/{}", owner, repo);
            return new ReadmeResult.Error("GitHub rate limit aşıldı veya erişim engellendi (403)");

        } catch (Exception e) {
            log.error("Beklenmeyen hata: {}/{} — {}", owner, repo, e.getMessage());
            return new ReadmeResult.Error("Beklenmeyen hata: " + e.getMessage());
        }
    }
}
