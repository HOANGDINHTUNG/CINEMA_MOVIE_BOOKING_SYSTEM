package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Movie;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieDisplayService {

    private final TmdbCatalogService tmdbCatalogService;

    public String resolveTitle(Movie movie, AppLanguage lang) {
        if (movie == null || movie.getTmdbId() == null) {
            return "Phim";
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
        return "TMDB #" + movie.getTmdbId();
    }

    public String resolveTitle(long tmdbId, AppLanguage lang) {
        try {
            var detail = tmdbCatalogService.getDetail(lang, tmdbId);
            if (detail.getTitle() != null && !detail.getTitle().isBlank()) {
                return detail.getTitle();
            }
        } catch (Exception ignored) {
        }
        return "TMDB #" + tmdbId;
    }
}
