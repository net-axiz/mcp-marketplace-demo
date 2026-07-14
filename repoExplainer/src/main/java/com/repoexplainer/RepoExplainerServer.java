package com.repoexplainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Repo Explainer MCP Sunucusu.
 * GitHub repolarını analiz edip açıklama üreten bir MCP bileşeni.
 */
@SpringBootApplication
public class RepoExplainerServer {

    public static void main(String[] args) {
        SpringApplication.run(RepoExplainerServer.class, args);
    }
}