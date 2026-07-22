package com.filereader.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalFileReadingService implements FileReadingService {

    private static final Path BASE_DIR = Paths.get("/").toAbsolutePath().normalize();

    @Override
    public String readFile(String fileName) throws IOException {
        Path resolved = BASE_DIR.resolve(fileName).normalize();

        if (!resolved.startsWith(BASE_DIR)) {
            throw new SecurityException("Access denied: path traversal detected");
        }

        if (!Files.exists(resolved)) {
            throw new IOException("File not found: " + fileName);
        }

        return Files.readString(resolved);
    }
}
