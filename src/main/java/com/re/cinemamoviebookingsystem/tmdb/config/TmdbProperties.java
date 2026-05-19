package com.re.cinemamoviebookingsystem.tmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmdb")
public record TmdbProperties(
        String baseUrl,
        String apiKey,
        String bearerToken,
        int timeoutMs,
        String imageBase
) {
    public boolean isConfigured() {
        boolean hasApiKey = apiKey != null && !apiKey.isBlank();
        boolean hasBearer = bearerToken != null && !bearerToken.isBlank();
        return hasApiKey || hasBearer;
    }

    /** V3 query param — uu tien api-key; bearer van gui header neu co. */
    public String effectiveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }
        return null;
    }
}
