package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.dto.response.ScheduleMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.SchedulePageApiResponse;
import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;
import com.re.cinemamoviebookingsystem.service.CinemaCatalogService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerSchedulePageService {

    private final ShowtimeService showtimeService;
    private final CinemaCatalogService cinemaCatalogService;
    private final TmdbCatalogService tmdbCatalogService;

    public SchedulePageApiResponse loadSchedule(
            AppLanguage appLanguage,
            LocalDate date,
            String q,
            String genre,
            String sort,
            List<String> age,
            List<String> format,
            List<String> origin,
            Boolean available) {

        List<TmdbGenreItemDto> tmdbGenres = List.of();
        try {
            tmdbGenres = tmdbCatalogService.listGenres(appLanguage);
        } catch (Exception ignored) {
        }

        String sortKey = SchedulePageSupport.normalizeSort(sort);
        List<String> selectedAges = SchedulePageSupport.normalizeAgeFilters(age);
        List<String> selectedFormats = SchedulePageSupport.normalizeFormatFilters(format);
        List<String> selectedOrigins = SchedulePageSupport.normalizeOriginFilters(origin);
        boolean availableOnly = Boolean.TRUE.equals(available);

        var schedule = showtimeService.buildScheduleView(date);
        List<ScheduleMovieCardDto> enrichedMovies = cinemaCatalogService.enrichScheduleMovies(
                schedule.getMovies(), appLanguage);
        enrichedMovies = TmdbGenreFilterSupport.filterSchedule(enrichedMovies, q, genre, tmdbGenres);
        enrichedMovies = SchedulePageSupport.applyExtraFilters(
                enrichedMovies, selectedAges, selectedFormats, selectedOrigins, availableOnly);
        enrichedMovies = SchedulePageSupport.sortMovies(enrichedMovies, sortKey);

        String genreParam = genre != null ? genre.trim() : "";
        boolean filtering = SchedulePageSupport.isFiltering(
                q, genreParam, selectedAges, selectedFormats, selectedOrigins, availableOnly);

        return SchedulePageApiResponse.builder()
                .days(schedule.getDays())
                .selectedDate(schedule.getSelectedDate())
                .movies(enrichedMovies)
                .resultCount(enrichedMovies.size())
                .filtering(filtering)
                .selectedGenreName(TmdbGenreFilterSupport.resolveGenreName(tmdbGenres, genreParam))
                .genres(tmdbGenres)
                .build();
    }
}
