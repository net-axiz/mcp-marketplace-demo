package com.gitmcp.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class LocalRepositoryDocumentService implements RepositoryDocumentService {

    @Override
    public List<String> listDocs(String repoPath) throws IOException {
        Path root = Paths.get(repoPath);
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(p -> p.toString().endsWith(".md"))
                    .map(p -> root.relativize(p).toString())
                    .toList();
        }
    }

    @Override
    public String readDoc(String repoPath, String fileName) throws IOException {
        Path root = Paths.get(repoPath).normalize().toAbsolutePath();
        Path fullPath = root.resolve(fileName).normalize();

        if (!fullPath.startsWith(root)) {
            throw new SecurityException("Erişim reddedildi");
        }

        return Files.readString(fullPath);
    }
}
