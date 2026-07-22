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
                log.info("README found: {}/{} ({} chars)", owner, repo, content.length());
                return new ReadmeResult.Found(content);
            } else {
                log.info("README not found: {}/{}", owner, repo);
                return new ReadmeResult.NotFound();
            }

        } catch (WebClientResponseException.Unauthorized e) {
            log.error("401 -- Invalid token: {}/{}", owner, repo);
            return new ReadmeResult.Error("Invalid GitHub token (401)");

        } catch (WebClientResponseException.Forbidden e) {
            log.error("403 -- Rate limit or access denied: {}/{}", owner, repo);
            return new ReadmeResult.Error("GitHub rate limit exceeded or access denied (403)");

        } catch (Exception e) {
            log.error("Unexpected error: {}/{} -- {}", owner, repo, e.getMessage());
            return new ReadmeResult.Error("Unexpected error: " + e.getMessage());
        }
    }
}
