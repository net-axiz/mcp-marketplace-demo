package com.repoexplainer.orchestration;

import com.repoexplainer.github.GitHubReadmeService;
import com.repoexplainer.github.GitHubUrlParser;
import com.repoexplainer.github.GitHubUrlParser.GitHubRepo;
import com.repoexplainer.github.model.ReadmeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class RepoExplainerService {

    private static final Logger log = LoggerFactory.getLogger(RepoExplainerService.class);

    private final GitHubReadmeService readmeService;
    private final FallbackScanner fallbackScanner;
    private final ChatClient chatClient;

    public RepoExplainerService(
            GitHubReadmeService readmeService,
            FallbackScanner fallbackScanner,
            ChatClient chatClient) {
        this.readmeService = readmeService;
        this.fallbackScanner = fallbackScanner;
        this.chatClient = chatClient;
    }


    public String explainRepo(String githubUrl) {
        GitHubRepo ghRepo = GitHubUrlParser.parse(githubUrl)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Geçersiz GitHub URL: " + githubUrl));

        String owner = ghRepo.owner();
        String repo = ghRepo.repo();
        log.info("Repo analiz ediliyor: {}/{}", owner, repo);

        ReadmeResult readmeResult = readmeService.fetchReadme(owner, repo);

        return switch (readmeResult) {
            case ReadmeResult.Found found -> {
                log.info("README bulundu, LLM'e gönderiliyor...");
                yield askLlmWithReadme(owner, repo, found.content());
            }
            case ReadmeResult.NotFound notFound -> {
                log.info("README yok, fallback tarama başlıyor...");
                String context = fallbackScanner.scan(owner, repo);
                yield askLlmWithFallback(owner, repo, context);
            }
            case ReadmeResult.Error error -> {
                log.error("README çekilemedi: {}", error.reason());
                yield "Hata: " + error.reason();
            }
        };
    }

    private String askLlmWithReadme(String owner, String repo, String readmeContent) {
        String safeContent = readmeContent;
        if (safeContent.length() > 5000) {
            safeContent = safeContent.substring(0, 5000) + "\n\n... (Content truncated due to length for LLM performance) ...";
            log.warn("README çok uzun, 5000 karaktere kısaltıldı: {}/{}", owner, repo);
        }

        String prompt = """
                Analyze the README file of the following GitHub repository: %s/%s

                README CONTENT:
                %s

                Please explain the following:
                1. What does this project do?
                2. Which technologies are used?
                3. How to install and run it?
                4. What is the general structure of the project?
                """.formatted(owner, repo, safeContent);

        return callLlm(prompt);
    }
    private String askLlmWithFallback(String owner, String repo, String context) {
        String safeContent = context;
        if (safeContent.length() > 5000) {
            safeContent = safeContent.substring(0, 5000) + "\n\n... (Content truncated due to length for LLM performance) ...";
            log.warn("Fallback içeriği çok uzun, 5000 karaktere kısaltıldı: {}/{}", owner, repo);
        }

        String prompt = """
                Analyze the following GitHub repository: %s/%s
                This repository does not have a README file. Below is the file tree and contents of some important files.

                %s

                Based on this information:
                1. What does this project do?
                2. Which technologies are used?
                3. What is the general structure of the project?

                IMPORTANT: Explicitly state if you are unsure about any points. If making a guess, use words like "probably" or "my guess is".
                """
                .formatted(owner, repo, safeContent);

        return callLlm(prompt);
    }

    private String callLlm(String prompt) {
        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM çağrısı başarısız: {}", e.getMessage());
            return "LLM çağrısı başarısız oldu: " + e.getMessage();
        }
    }
}
