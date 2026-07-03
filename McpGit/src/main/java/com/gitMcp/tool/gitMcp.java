package com.gitMcp.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;




@Component
public class gitMcp {
    @McpTool(name = "commitCommand", description = "Commiting method.")
    public void commitCommand(
        @McpToolParam(description = "Commit Text", required = true)String commitText, 
        @McpToolParam(description = "Author", required = true)String authorText){
        Git git = new Git(db);
        CommitCommand commit = git.commit();
        commit.setMessage(commitText).setAuthor(authorText).call();}
    
    @McpTool(name = "Clone a repo")
    public void cloneRepo(
        @McpToolParam(description ="Github Repo Link", required = true)String uri,
        @McpToolParam(description = "File directory path for clonning repo", required = true)String path){
        Git git = Git.cloneRepository().setURI(uri).setDirectory(path).call();}



}
