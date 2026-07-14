package com.gitmcp.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class DefaultBranchNamingService implements BranchNamingService {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9\\s-]");
    private static final Pattern MULTIPLE_SPACES_OR_DASHES = Pattern.compile("[\\s-]+");

    @Override
    public String generate(String storyId, String title) {
        String sanitizedTitle = sanitize(title);
        return String.format("feature/%s-%s", storyId, sanitizedTitle);
    }

    private String sanitize(String input) {
        String result = input.toLowerCase();
        result = NON_ALPHANUMERIC.matcher(result).replaceAll("");
        result = MULTIPLE_SPACES_OR_DASHES.matcher(result).replaceAll("-");
        return result.strip();
    }
}
