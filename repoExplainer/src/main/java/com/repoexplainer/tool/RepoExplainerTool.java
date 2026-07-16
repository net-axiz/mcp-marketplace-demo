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

    @McpTool(description = "Analyzes a given GitHub repository and generates an English explanation. "
            + "Summarizes the README if available, otherwise scans important files to explain the repo.")
    public String explainRepo(
            @McpToolParam(description = "GitHub repo link (e.g.: https://github.com/owner/repo)") String githubUrl) {
        return service.explainRepo(githubUrl);
    }
}
