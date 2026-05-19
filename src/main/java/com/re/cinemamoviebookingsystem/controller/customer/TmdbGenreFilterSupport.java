package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.dto.response.ScheduleMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.CinemaMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;

import java.util.List;
import java.util.Locale;

final class TmdbGenreFilterSupport {

    private TmdbGenreFilterSupport() {
    }

    static Integer parseGenreId(String genreParam) {
        if (genreParam == null || genreParam.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(genreParam.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static String resolveGenreName(List<TmdbGenreItemDto> genres, String genreParam) {
        Integer id = parseGenreId(genreParam);
        if (id != null && genres != null) {
            return genres.stream()
                    .filter(g -> id.equals(g.getId()))
                    .map(TmdbGenreItemDto::getName)
                    .findFirst()
                    .orElse(null);
        }
        if (genreParam != null && !genreParam.isBlank()) {
            return genreParam.trim();
        }
        return null;
    }

    static List<CinemaMovieCardDto> filterByTitle(List<CinemaMovieCardDto> cards, String keyword) {
        if (keyword == null || keyword.isBlank() || cards == null) {
            return cards != null ? cards : List.of();
        }
        String q = keyword.trim().toLowerCase(Locale.ROOT);
        return cards.stream()
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    static List<ScheduleMovieCardDto> filterSchedule(List<ScheduleMovieCardDto> cards, String keyword, String genreParam,
                                                     List<TmdbGenreItemDto> genres) {
        if (cards == null) {
            return List.of();
        }
        List<ScheduleMovieCardDto> result = cards;
        if (keyword != null && !keyword.isBlank()) {
            String q = keyword.trim().toLowerCase(Locale.ROOT);
            result = result.stream()
                    .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(q))
                    .toList();
        }
        String genreName = resolveGenreName(genres, genreParam);
        if (genreName != null && !genreName.isBlank()) {
            String g = genreName.toLowerCase(Locale.ROOT);
            result = result.stream()
                    .filter(c -> c.getGenresLabel() != null
                            && c.getGenresLabel().toLowerCase(Locale.ROOT).contains(g))
                    .toList();
        }
        return result;
    }
}
