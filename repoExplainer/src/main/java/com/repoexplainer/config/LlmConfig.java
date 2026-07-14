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
                        Sen bir GitHub repo açıklayıcısısın.
                        Sana verilen repo bilgilerini (README veya dosya içerikleri) analiz et.
                        Şu soruları cevapla:
                        1. Bu proje ne işe yarıyor?
                        2. Hangi teknolojileri kullanıyor?
                        3. Nasıl kurulur ve çalıştırılır?
                        4. Projenin genel yapısı nasıl?

                        Cevabını Türkçe ver. Kısa ve net yaz.
                        Emin olmadığın noktaları açıkça belirt.
                        """)
                .build();
    }
}
