package com.re.cinemamoviebookingsystem.api;

import com.re.cinemamoviebookingsystem.dto.response.ShowtimeBrowseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/cinema")
@RequiredArgsConstructor
public class PublicCinemaController {

    private final TmdbCatalogService tmdbCatalogService;
    private final ShowtimeService showtimeService;

    @GetMapping("/movies/{tmdbId}")
    public MovieCatalogDetailDto getMovie(
            @PathVariable long tmdbId,
            @RequestParam(defaultValue = "vi-VN") String lang) {
        return tmdbCatalogService.getDetail(AppLanguage.fromParam(lang), tmdbId);
    }

    @GetMapping("/movies/{tmdbId}/showtimes")
    public List<ShowtimeBrowseDto> showtimes(@PathVariable long tmdbId) {
        return showtimeService.listByTmdbId(tmdbId);
    }
}
