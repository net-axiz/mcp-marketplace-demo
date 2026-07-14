package com.FileReader.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class LocalFileReadingService implements FileReadingService {

    @Override
    public String readFile(String fileName) throws Exception {
        File file = new File(fileName);
        Path path = file.toPath();
        return Files.readString(path);
    }
}
