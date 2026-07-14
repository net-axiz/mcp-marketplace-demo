package com.repoexplainer.tool;

import com.repoexplainer.orchestration.RepoExplainerService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class RepoExplainerTool {

    private final RepoExplainerService service;

    public RepoExplainerTool(RepoExplainerService service) {
        this.service = service;
    }

    @McpTool(description = "Verilen GitHub reposunu analiz eder ve Türkçe bir açıklama üretir. "
            + "README varsa onu özetler, yoksa önemli dosyaları tarayarak repoyu açıklar.")
    public String explainRepo(
            @McpToolParam(description = "GitHub repo linki (örn: https://github.com/owner/repo)") String githubUrl) {
        return service.explainRepo(githubUrl);
    }
}
