package com.gitmcp.tool;

import com.gitmcp.service.BranchNamingService;
import com.gitmcp.service.RepositoryDocumentService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class GitMcp {

    private final BranchNamingService branchNamingService;
    private final RepositoryDocumentService documentService;

    public GitMcp(BranchNamingService branchNamingService, RepositoryDocumentService documentService) {
        this.branchNamingService = branchNamingService;
        this.documentService = documentService;
    }

    @McpTool(name = "List Docs", description = "List available documentation files in a given repo path")
    public List<String> listDocs(
        @McpToolParam(description = "Absolute path to the repository", required = true) String repoPath) throws IOException {
        return documentService.listDocs(repoPath);
    }

    @McpTool(name = "Read Doc", description = "Read content of a documentation file")
    public String readDoc(
        @McpToolParam(description = "Absolute path to the repository", required = true) String repoPath,
        @McpToolParam(description = "Relative file path within the repo", required = true) String fileName)
        throws IOException {
        return documentService.readDoc(repoPath, fileName);
    }

    @McpTool(name = "Branch Name Generator", description="Generates Branch Names.")
    public String generateBranchName(
        @McpToolParam(description = "Issue or Story ID.", required = true) String storyID,
        @McpToolParam(description = "Summary of changes for the branch name.", required = true) String title) {
        return branchNamingService.generate(storyID, title);
    }
}