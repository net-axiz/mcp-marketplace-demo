package com.filereader.service;

import java.io.IOException;

public interface FileReadingService {
    String readFile(String fileName) throws IOException;
}
