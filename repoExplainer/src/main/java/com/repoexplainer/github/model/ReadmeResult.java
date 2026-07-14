package com.repoexplainer.github.model;

/**
 * README çekme işleminin sonucunu temsil eden sealed interface.
 *
 * Neden sealed interface?
 * - 3 farklı durum var: bulundu, bulunamadı, hata.
 * - Optional<String> bu 3 durumu ayırt edemez.
 * - Sealed interface + record ile her durum ayrı bir tip oluyor.
 * - switch ile kullanınca derleyici eksik dalı yakalıyor.
 *
 * Kullanım:
 * switch (result) {
 * case Found f -> f.content(); // README içeriği
 * case NotFound n -> "README yok"; // Normal durum, hata değil
 * case Error e -> "Hata: " + e.reason(); // Gerçek hata
 * }
 */
public sealed interface ReadmeResult {

    /**
     * README bulundu. content = README'nin metin içeriği (base64'ten decode
     * edilmiş).
     */
    record Found(String content) implements ReadmeResult {
    }

    /** README bulunamadı. Bu bir hata değil — repo'da README dosyası yok demek. */
    record NotFound() implements ReadmeResult {
    }

    /**
     * Gerçek bir hata oluştu. reason = hata açıklaması (rate limit, token sorunu
     * vb.).
     */
    record Error(String reason) implements ReadmeResult {
    }
}
