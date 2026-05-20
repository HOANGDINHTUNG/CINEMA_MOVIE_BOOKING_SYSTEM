package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Movie;
import com.re.cinemamoviebookingsystem.repository.MovieRepository;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieDisplayService {

    private final TmdbCatalogService tmdbCatalogService;
    private final MovieRepository movieRepository;

    public String resolveTitle(Movie movie, AppLanguage lang) {
        if (movie == null) {
            return "Phim";
        }
        String local = resolveTitleLocal(movie);
        if (local != null && !local.startsWith("TMDB #")) {
            return local;
        }
        if (movie.getTmdbId() == null) {
            return local != null ? local : "Phim";
        }
        try {
            var detail = tmdbCatalogService.getDetail(lang, movie.getTmdbId());
            if (detail.getTitle() != null && !detail.getTitle().isBlank()) {
                return detail.getTitle();
            }
            if (detail.getOriginalTitle() != null && !detail.getOriginalTitle().isBlank()) {
                return detail.getOriginalTitle();
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return local != null ? local : "TMDB #" + movie.getTmdbId();
    }

    public String resolveTitle(long tmdbId, AppLanguage lang) {
        return movieRepository.findByTmdbId(tmdbId)
                .map(m -> resolveTitle(m, lang))
                .orElseGet(() -> resolveTitleFromTmdbOnly(tmdbId, lang));
    }

    private String resolveTitleFromTmdbOnly(long tmdbId, AppLanguage lang) {
        try {
            var detail = tmdbCatalogService.getDetail(lang, tmdbId);
            if (detail.getTitle() != null && !detail.getTitle().isBlank()) {
                return detail.getTitle();
            }
            if (detail.getOriginalTitle() != null && !detail.getOriginalTitle().isBlank()) {
                return detail.getOriginalTitle();
            }
        } catch (Exception ignored) {
        }
        return "TMDB #" + tmdbId;
    }

    public static String titleFromAdminNote(String adminNote) {
        if (adminNote == null || adminNote.isBlank()) {
            return null;
        }
        int colon = adminNote.indexOf(':');
        if (colon >= 0 && colon < adminNote.length() - 1) {
            return adminNote.substring(colon + 1).trim();
        }
        return adminNote.trim();
    }

    /**
     * Fast, local-only title resolution (no TMDB call) for latency-sensitive flows.
     */
    public String resolveTitleLocal(Movie movie) {
        if (movie == null) {
            return "Phim";
        }
        String fromNote = titleFromAdminNote(movie.getAdminNote());
        if (fromNote != null) {
            return fromNote;
        }
        if (movie.getTmdbId() != null) {
            return "TMDB #" + movie.getTmdbId();
        }
        return "Phim";
    }
}
