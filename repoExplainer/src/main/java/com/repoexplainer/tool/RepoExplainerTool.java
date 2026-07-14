package com.repoexplainer.tool;

import com.repoexplainer.orchestration.RepoExplainerService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP Tool katmanı — orkestratörü dışarıya açan araç.
 *
 * Ne yapar:
 * - Bu sınıf bir MCP Tool olarak kayıtlıdır.
 * - Dışarıdan "explainRepo" metodu çağrılabilir.
 * - İçeride RepoExplainerService'i çağırır, sonucu döndürür.
 *
 * MCP client'lar (örn: Claude Desktop, başka bir MCP client) bu aracı keşfedip
 * kullanabilir.
 */
@Component
public class RepoExplainerTool {

    private final RepoExplainerService service;

    public RepoExplainerTool(RepoExplainerService service) {
        this.service = service;
    }

    /**
     * GitHub reposunu analiz edip Türkçe açıklama üretir.
     *
     * @param githubUrl GitHub linki. Desteklenen formatlar:
     *                  - https://github.com/owner/repo
     *                  - owner/repo
     *                  - https://github.com/owner/repo.git
     * @return Repo'nun LLM tarafından üretilmiş açıklaması
     */
    @McpTool(description = "Verilen GitHub reposunu analiz eder ve Türkçe bir açıklama üretir. "
            + "README varsa onu özetler, yoksa önemli dosyaları tarayarak repoyu açıklar.")
    public String explainRepo(
            @McpToolParam(description = "GitHub repo linki (örn: https://github.com/owner/repo)") String githubUrl) {
        return service.explainRepo(githubUrl);
    }
}
