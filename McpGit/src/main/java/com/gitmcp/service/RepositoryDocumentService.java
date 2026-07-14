package com.gitmcp.service;

import java.io.IOException;
import java.util.List;

public interface RepositoryDocumentService {
    List<String> listDocs(String repoPath) throws IOException;
    String readDoc(String repoPath, String fileName) throws IOException;
}
