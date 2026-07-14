package com.FileReader.tool;

import com.FileReader.service.FileReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class FileReaderTool {

    private static final Logger log = LoggerFactory.getLogger(FileReaderTool.class);
    
    private final FileReadingService fileReadingService;

    public FileReaderTool(FileReadingService fileReadingService) {
        this.fileReadingService = fileReadingService;
    }

    @McpTool(name = "read", description = "Reads files.")
    public String readFile(@McpToolParam(description = "File Name", required = true) String fileName) {
        try {
            return fileReadingService.readFile(fileName);
        } catch (Exception e) {
            log.error("File cannot read", e); 
            return "Cant Find: " + e.getMessage();
        }
    }
}
