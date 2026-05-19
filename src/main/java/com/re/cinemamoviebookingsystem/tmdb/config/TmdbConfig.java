package com.re.cinemamoviebookingsystem.tmdb.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
@EnableCaching
public class TmdbConfig {

    @Bean
    public RestClient tmdbRestClient(TmdbProperties properties) {
        String baseUrl = properties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.themoviedb.org/3";
        }
        // Khong them '/' cuoi — path tu client khong bat dau bang '/' de ghep dung .../3/discover/movie
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json");

        if (properties.bearerToken() != null && !properties.bearerToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.bearerToken());
        }

        return builder.build();
    }
}
