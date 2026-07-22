package com.repoexplainer.github;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);

    private final WebClient webClient;

    public GitHubApiClient(WebClient githubWebClient) {
        this.webClient = githubWebClient;
    }
    public String fetchReadme(String owner, String repo) {
        try {
            JsonNode response = webClient.get()
                    .uri("/repos/{owner}/{repo}/readme", owner, repo)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("content")) {
                String base64Content = response.get("content").asText();
                return decodeBase64(base64Content);
            }
            return null;

        } catch (WebClientResponseException.NotFound e) {
            log.debug("README not found: {}/{}", owner, repo);
            return null;
        }
    }

    public String fetchDefaultBranch(String owner, String repo) {
        JsonNode response = webClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response != null && response.has("default_branch")) {
            return response.get("default_branch").asText();
        }
        return "main"; // Fallback
    }

    public List<String> fetchTree(String owner, String repo, String branch) {
        List<String> paths = new ArrayList<>();

        try {
            JsonNode response = webClient.get()
                    .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, branch)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("tree")) {
                for (JsonNode node : response.get("tree")) {
                    if ("blob".equals(node.get("type").asText())) {
                        paths.add(node.get("path").asText());
                    }
                }
            }
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch file tree: {}/{} branch={} -- {}", owner, repo, branch, e.getMessage());
        }

        return paths;
    }
    public String fetchFileContent(String owner, String repo, String path) {
        try {
            JsonNode response = webClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("content")) {
                String base64Content = response.get("content").asText();
                return decodeBase64(base64Content);
            }
            return null;

        } catch (WebClientResponseException e) {
            log.warn("Failed to fetch file content: {}/{}/{} -- {}", owner, repo, path, e.getMessage());
            return null;
        }
    }

    private String decodeBase64(String base64Content) {
        String cleaned = base64Content.replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(cleaned);
        return new String(decoded, StandardCharsets.UTF_8);
    }
}
