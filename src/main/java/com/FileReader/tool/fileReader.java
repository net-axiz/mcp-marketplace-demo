package com.FileReader.tool;

import java.util.logging.Logger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class fileReader {

    private static final Logger log = (java.util.logging.Logger) LoggerFactory.getLogger(fileReader.class);

    @McpTool(name = "read", description = "Reads files.")
    public String readFile(@McpToolParam(description = "File Name", required = true) String fileName) {
        try{Path filePath = Path.of(fileName);
        return Files.readString(filePath);} 

        catch (Exception e){log.error("File cannot read", e);
            return "This is Error: " + e.getMessage();     
        }
        

    }
}
