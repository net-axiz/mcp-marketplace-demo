package com.repoexplainer.orchestration;

import com.repoexplainer.github.GitHubApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class FallbackScanner {

    private static final Logger log = LoggerFactory.getLogger(FallbackScanner.class);

    private static final int CHARACTER_BUDGET = 18_000;

    private final GitHubApiClient apiClient;

    private static final Set<String> MANIFEST_FILES = Set.of(
            "pom.xml", "build.gradle", "build.gradle.kts",
            "package.json", "requirements.txt", "setup.py", "pyproject.toml",
            "Cargo.toml", "go.mod", "Gemfile", "composer.json"
    );


    private static final Set<String> DOCKER_FILES = Set.of(
            "Dockerfile", "docker-compose.yml", "docker-compose.yaml"
    );

    private static final Set<String> ENTRY_POINT_FILES = Set.of(
            "Main.java", "Application.java",
            "main.py", "app.py",
            "index.js", "index.ts", "main.js", "main.ts",
            "main.go", "main.rs"
    );

    public FallbackScanner(GitHubApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public String scan(String owner, String repo) {
        String branch = apiClient.fetchDefaultBranch(owner, repo);
        log.info("Varsayılan branch: {} ({}/{})", branch, owner, repo);

        List<String> allPaths = apiClient.fetchTree(owner, repo, branch);
        log.info("Toplam {} dosya bulundu", allPaths.size());

        List<String> prioritizedPaths = prioritize(allPaths);
        log.info("Önceliklendirilmiş {} dosya seçildi", prioritizedPaths.size());

        StringBuilder context = new StringBuilder();
        int usedBudget = 0;

        String treeOverview = buildTreeOverview(allPaths);
        context.append("=== DİZİN AĞACI ===\n");
        context.append(treeOverview);
        context.append("\n\n");
        usedBudget += treeOverview.length();

        for (String path : prioritizedPaths) {
            if (usedBudget >= CHARACTER_BUDGET) {
                log.info("Karakter bütçesi doldu ({}/{}), kalan dosyalar atlanıyor", usedBudget, CHARACTER_BUDGET);
                break;
            }

            String content = apiClient.fetchFileContent(owner, repo, path);
            if (content == null || content.isBlank()) {
                continue;
            }

            int remaining = CHARACTER_BUDGET - usedBudget;
            if (content.length() > remaining) {
                content = content.substring(0, remaining) + "\n... (kısaltıldı)";
            }

            context.append("=== ").append(path).append(" ===\n");
            context.append(content);
            context.append("\n\n");
            usedBudget += content.length() + path.length() + 10;
        }

        log.info("Toplam {} karakter bağlam toplandı", usedBudget);
        return context.toString();
    }

    List<String> prioritize(List<String> allPaths) {
        List<String> result = new ArrayList<>();

        for (String path : allPaths) {
            String fileName = getFileName(path);
            if (MANIFEST_FILES.contains(fileName)) {
                result.add(path);
            }
        }

        for (String path : allPaths) {
            if (!path.contains("/") && path.endsWith(".md")
                    && !path.equalsIgnoreCase("README.md")) {
                result.add(path);
            }
        }

        for (String path : allPaths) {
            String fileName = getFileName(path);
            if (DOCKER_FILES.contains(fileName)) {
                result.add(path);
            }
        }

        for (String path : allPaths) {
            String fileName = getFileName(path);
            if (ENTRY_POINT_FILES.contains(fileName)) {
                result.add(path);
            }
        }

        return result.stream().distinct().collect(Collectors.toList());
    }


    private String buildTreeOverview(List<String> paths) {
        int limit = Math.min(paths.size(), 100);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            sb.append(paths.get(i)).append("\n");
        }
        if (paths.size() > limit) {
            sb.append("... ve ").append(paths.size() - limit).append(" dosya daha\n");
        }
        return sb.toString();
    }

    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
