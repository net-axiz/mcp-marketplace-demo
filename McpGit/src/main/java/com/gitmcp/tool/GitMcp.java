package com.gitmcp.tool;

import com.gitmcp.service.BranchNameGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;




@Component
public class GitMcp {
    
    @McpTool(name = "List Docs", description = "List available documentation files in a given repo path")
    public List<String> listDocs(
        @McpToolParam(description = "Absolute path to the repository", required = true) String repoPath) throws IOException { Path root = Paths.get(repoPath);
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> p.toString().endsWith(".md")).map(p -> root.relativize(p).toString()).toList();}}

    @McpTool(name = "Read Doc", description = "Read content of a documentation file")    
    public String readDoc(
        @McpToolParam(description = "Absolute path to the repository", required = true) String repoPath,
        @McpToolParam(description = "Relative file path within the repo", required = true) String fileName) 
        throws IOException { Path root = Paths.get(repoPath).normalize().toAbsolutePath(); Path fullPath = root.resolve(fileName).normalize();

        if (!fullPath.startsWith(root)) { throw new SecurityException("Erişim reddedildi");}

        return Files.readString(fullPath);}

    @McpTool(name = "Branch Name Generator", description="Generates Branch Names.")
    public String generateBranchName(
        @McpToolParam(description = "Summary of changes.", required = true) String  storyID,
        @McpToolParam(description = "Relative issue ID.", required = true) String title)
        {return BranchNameGenerator.generate(storyID , title); }
    }   