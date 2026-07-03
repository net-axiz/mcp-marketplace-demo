package com.FileReader.tool;

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

    private static final Logger log = LoggerFactory.getLogger(fileReader.class);

    @McpTool(name = "read", description = "Reads files.")
    public String readFile(@McpToolParam(description = "File Name", required = true) String fileName) {
        try{
            File file = new File(fileName);
            Path path = file.toPath();
            return Files.readString(path);}

        catch (Exception e){
            log.error ("File cannot read", e); return "Cant Find: " + e.getMessage();}}}

/* her ne kadar file path girmen gerekse de bir şekilde de olsa sorunu çözdüm bunu gidip obsidiana not aldım. */