package com.re.cinemamoviebookingsystem.tmdb.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.re.cinemamoviebookingsystem.tmdb.config.TmdbProperties;
import com.re.cinemamoviebookingsystem.tmdb.dto.*;
import com.re.cinemamoviebookingsystem.tmdb.exception.TmdbApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Year;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TmdbClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient tmdbRestClient;
    private final TmdbProperties properties;

    public TmdbPagedResponseDto<TmdbMovieSummaryDto> discoverLatest(String language, int page) {
        return discoverQuality(language, page, null);
    }

    /**
     * Phim có đủ metadata: poster, overview, điểm đánh giá, lọc theo thể loại TMDB (with_genres).
     */
    public TmdbPagedResponseDto<TmdbMovieSummaryDto> discoverQuality(String language, int page, Integer genreId) {
        java.util.HashMap<String, String> params = new java.util.HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("language", language);
        params.put("sort_by", "popularity.desc");
        params.put("vote_count.gte", "80");
        params.put("vote_average.gte", "5.5");
        params.put("include_adult", "false");
        params.put("include_video", "false");
        if (genreId != null) {
            params.put("with_genres", String.valueOf(genreId));
        }
        return getPaged("/discover/movie", params);
    }

    public TmdbPagedResponseDto<TmdbMovieSummaryDto> nowPlaying(String language, int page) {
        return getPaged("/movie/now_playing", Map.of(
                "page", String.valueOf(page),
                "language", language
        ));
    }

    public TmdbPagedResponseDto<TmdbMovieSummaryDto> upcoming(String language, int page) {
        return getPaged("/movie/upcoming", Map.of(
                "page", String.valueOf(page),
                "language", language
        ));
    }

    public TmdbPagedResponseDto<TmdbMovieSummaryDto> trending(String language, int page, String window) {
        String path = "week".equalsIgnoreCase(window)
                ? "/trending/movie/week"
                : "/trending/movie/day";
        return getPaged(path, Map.of(
                "page", String.valueOf(page),
                "language", language
        ));
    }

    public TmdbPagedResponseDto<TmdbMovieSummaryDto> searchMovies(String language, int page, String query) {
        return getPaged("/search/movie", Map.of(
                "page", String.valueOf(page),
                "language", language,
                "query", query,
                "include_adult", "false"
        ));
    }

    public TmdbPagedResponseDto<TmdbMovieSummaryDto> discoverCurrentYear(String language, int page) {
        int year = Year.now().getValue();
        return getPaged("/discover/movie", Map.of(
                "page", String.valueOf(page),
                "language", language,
                "sort_by", "primary_release_date.desc",
                "primary_release_year", String.valueOf(year),
                "vote_count.gte", "50",
                "include_adult", "false",
                "include_video", "false"
        ));
    }

    public TmdbMovieDetailsDto getMovieDetails(long tmdbId, String language) {
        String append = "videos,credits,images,similar,recommendations";
        return get("/movie/" + tmdbId, Map.of(
                "language", language,
                "append_to_response", append
        ), TmdbMovieDetailsDto.class);
    }

    public TmdbGenreListResponseDto listGenres(String language) {
        return get("/genre/movie/list", Map.of("language", language), TmdbGenreListResponseDto.class);
    }

    private TmdbPagedResponseDto<TmdbMovieSummaryDto> getPaged(String path, Map<String, String> params) {
        String body = fetchBody(path, params);
        try {
            return MAPPER.readValue(body, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new TmdbApiException("Không parse được phản hồi TMDB", ex);
        }
    }

    private <T> T get(String path, Map<String, String> params, Class<T> type) {
        String body = fetchBody(path, params);
        try {
            return MAPPER.readValue(body, type);
        } catch (Exception ex) {
            throw new TmdbApiException("Không parse được phản hồi TMDB", ex);
        }
    }

    private String fetchBody(String path, Map<String, String> params) {
        ensureConfigured();
        URI uri = buildAbsoluteUri(path, params);
        try {
            return tmdbRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String detail = body != null && !body.isBlank()
                    ? body.substring(0, Math.min(200, body.length()))
                    : ex.getStatusText();
            throw new TmdbApiException(
                    "TMDB lỗi " + ex.getStatusCode().value() + ": " + detail,
                    ex.getStatusCode().value()
            );
        } catch (Exception ex) {
            throw new TmdbApiException("Không kết nối được TMDB: " + ex.getMessage(), ex);
        }
    }

    private URI buildAbsoluteUri(String path, Map<String, String> params) {
        String base = properties.baseUrl();
        if (base == null || base.isBlank()) {
            base = "https://api.themoviedb.org/3";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String relativePath = normalizePath(path);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base)
                .path("/" + relativePath);
        String apiKey = properties.effectiveApiKey();
        if (apiKey != null) {
            builder.queryParam("api_key", apiKey);
        }
        if (params != null) {
            params.forEach(builder::queryParam);
        }
        return builder.build().encode().toUri();
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new TmdbApiException(
                    "Chưa cấu hình TMDB. Tạo src/main/resources/application-local.properties với tmdb.api-key=... (hoặc TMDB_API_KEY)."
            );
        }
        if (properties.effectiveApiKey() == null && (properties.bearerToken() == null || properties.bearerToken().isBlank())) {
            throw new TmdbApiException(
                    "Thiếu tmdb.api-key (v3). Bearer token một mình không đủ cho endpoint discover/movie — thêm api-key trong application-local.properties."
            );
        }
    }
}
