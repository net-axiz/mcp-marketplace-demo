package com.repoexplainer.github.model;

public sealed interface ReadmeResult {

    record Found(String content) implements ReadmeResult {
    }

    record NotFound() implements ReadmeResult {
    }

    record Error(String reason) implements ReadmeResult {
    }
}
