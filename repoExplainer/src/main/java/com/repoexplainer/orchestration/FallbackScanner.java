package com.repoexplainer.orchestration;

import com.repoexplainer.github.GitHubApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * README olmadığında devreye giren yedek tarama mantığı.
 *
 * Ne yapar:
 * - Repo'nun dosya ağacını çeker.
 * - Önemli dosyaları öncelik sırasına göre seçer.
 * - Her dosyanın içeriğini çeker.
 * - Toplam karakter sayısını bir bütçe ile sınırlar (taşma önlenir).
 *
 * Öncelik sırası (yol haritası 4.2):
 * 1. Manifest dosyaları (pom.xml, package.json vb.)
 * 2. Kök dizindeki .md dosyaları
 * 3. Docker dosyaları
 * 4. Giriş noktası dosyaları (Main.java, index.js vb.)
 * 5. Dizin ağacının kendisi (sadece dosya yolları)
 */
@Component
public class FallbackScanner {

    private static final Logger log = LoggerFactory.getLogger(FallbackScanner.class);

    /** Toplam karakter bütçesi. Bu sınırı aşan dosyalar elenir. */
    private static final int CHARACTER_BUDGET = 18_000;

    private final GitHubApiClient apiClient;

    /** Manifest/bağımlılık dosyaları — en yüksek öncelik */
    private static final Set<String> MANIFEST_FILES = Set.of(
            "pom.xml", "build.gradle", "build.gradle.kts",
            "package.json", "requirements.txt", "setup.py", "pyproject.toml",
            "Cargo.toml", "go.mod", "Gemfile", "composer.json"
    );

    /** Docker dosyaları */
    private static final Set<String> DOCKER_FILES = Set.of(
            "Dockerfile", "docker-compose.yml", "docker-compose.yaml"
    );

    /** Giriş noktası dosyaları (dosya adı ile eşleşir) */
    private static final Set<String> ENTRY_POINT_FILES = Set.of(
            "Main.java", "Application.java",
            "main.py", "app.py",
            "index.js", "index.ts", "main.js", "main.ts",
            "main.go", "main.rs"
    );

    public FallbackScanner(GitHubApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Repo'yu tarayıp önemli dosyaların içeriğini toplar.
     *
     * @param owner Repo sahibi
     * @param repo  Repo adı
     * @return Toplanan bağlam metni (dosya adları + içerikleri birleştirilmiş)
     */
    public String scan(String owner, String repo) {
        // 1. Varsayılan branch'i öğren
        String branch = apiClient.fetchDefaultBranch(owner, repo);
        log.info("Varsayılan branch: {} ({}/{})", branch, owner, repo);

        // 2. Dosya ağacını çek
        List<String> allPaths = apiClient.fetchTree(owner, repo, branch);
        log.info("Toplam {} dosya bulundu", allPaths.size());

        // 3. Dosyaları önceliğe göre sırala
        List<String> prioritizedPaths = prioritize(allPaths);
        log.info("Önceliklendirilmiş {} dosya seçildi", prioritizedPaths.size());

        // 4. Budget dahilinde içerikleri çek
        StringBuilder context = new StringBuilder();
        int usedBudget = 0;

        // Önce dizin ağacını ekle (her zaman faydalı)
        String treeOverview = buildTreeOverview(allPaths);
        context.append("=== DİZİN AĞACI ===\n");
        context.append(treeOverview);
        context.append("\n\n");
        usedBudget += treeOverview.length();

        // Sonra öncelikli dosyaları tek tek çek
        for (String path : prioritizedPaths) {
            if (usedBudget >= CHARACTER_BUDGET) {
                log.info("Karakter bütçesi doldu ({}/{}), kalan dosyalar atlanıyor", usedBudget, CHARACTER_BUDGET);
                break;
            }

            String content = apiClient.fetchFileContent(owner, repo, path);
            if (content == null || content.isBlank()) {
                continue;
            }

            // Dosya çok uzunsa kısalt
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

    /**
     * Dosyaları öncelik sırasına göre sıralar.
     * Yüksek öncelikli dosyalar listenin başına gelir.
     */
    List<String> prioritize(List<String> allPaths) {
        List<String> result = new ArrayList<>();

        // Öncelik 1: Manifest dosyaları
        for (String path : allPaths) {
            String fileName = getFileName(path);
            if (MANIFEST_FILES.contains(fileName)) {
                result.add(path);
            }
        }

        // Öncelik 2: Kök dizindeki .md dosyaları (README hariç, o zaten denenmiş)
        for (String path : allPaths) {
            if (!path.contains("/") && path.endsWith(".md")
                    && !path.equalsIgnoreCase("README.md")) {
                result.add(path);
            }
        }

        // Öncelik 3: Docker dosyaları
        for (String path : allPaths) {
            String fileName = getFileName(path);
            if (DOCKER_FILES.contains(fileName)) {
                result.add(path);
            }
        }

        // Öncelik 4: Giriş noktası dosyaları
        for (String path : allPaths) {
            String fileName = getFileName(path);
            if (ENTRY_POINT_FILES.contains(fileName)) {
                result.add(path);
            }
        }

        // Tekrar edenleri kaldır (bir dosya birden fazla kategoriye girebilir)
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Dosya yollarından kısa bir dizin ağacı özeti oluşturur.
     * LLM'e "repo'da hangi dosyalar var" bilgisi verir.
     */
    private String buildTreeOverview(List<String> paths) {
        // Çok fazla dosya varsa ilk 100'ü göster
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

    /** Yoldan dosya adını çıkarır. Örn: "src/main/Main.java" → "Main.java" */
    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
