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
        String prompt = """
                Şu GitHub reposunun README dosyasını analiz et: %s/%s

                README İÇERİĞİ:
                %s

                Lütfen şunları açıkla:
                1. Proje ne işe yarıyor?
                2. Hangi teknolojiler kullanılmış?
                3. Nasıl kurulur ve çalıştırılır?
                4. Projenin genel yapısı nasıl?
                """.formatted(owner, repo, readmeContent);

        return callLlm(prompt);
    }
    private String askLlmWithFallback(String owner, String repo, String context) {
        String prompt = """
                Şu GitHub reposunu analiz et: %s/%s
                Bu repoda README dosyası yok. Aşağıda dosya ağacı ve bazı önemli dosyaların içerikleri var.

                %s

                Bu bilgilere dayanarak:
                1. Proje ne işe yarıyor?
                2. Hangi teknolojiler kullanılmış?
                3. Projenin genel yapısı nasıl?

                ÖNEMLİ: Emin olmadığın noktaları açıkça belirt. Tahmin yapıyorsan "muhtemelen" veya "tahminim" gibi kelimeler kullan.
                """
                .formatted(owner, repo, context);

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
