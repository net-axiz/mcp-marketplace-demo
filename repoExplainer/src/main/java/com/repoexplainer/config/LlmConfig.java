package com.repoexplainer.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        You are a GitHub repository explainer.
                        Analyze the given repository information (README or file contents).
                        Answer the following questions:
                        1. What does this project do?
                        2. Which technologies does it use?
                        3. How to install and run it?
                        4. What is the general structure of the project?

                        Provide your answer in English. Be concise and clear.
                        Explicitly state if you are unsure about any points.
                        """)
                .build();
    }
}
