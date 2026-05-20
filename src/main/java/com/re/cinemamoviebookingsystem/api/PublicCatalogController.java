package com.re.cinemamoviebookingsystem.api;

import com.re.cinemamoviebookingsystem.dto.response.catalog.CinemaMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.HomeBootstrapResponseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.HomeMoviesResponseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogPageDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;
import com.re.cinemamoviebookingsystem.service.TmdbHomeCatalogService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final TmdbCatalogService tmdbCatalogService;
    private final TmdbHomeCatalogService tmdbHomeCatalogService;

    @GetMapping("/movies/discover")
    public MovieCatalogPageDto discover(
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbCatalogService.discoverLatest(AppLanguage.fromParam(lang), page);
    }

    @GetMapping("/movies/now-playing")
    public MovieCatalogPageDto nowPlaying(
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbCatalogService.nowPlaying(AppLanguage.fromParam(lang), page);
    }

    @GetMapping("/movies/upcoming")
    public MovieCatalogPageDto upcoming(
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbCatalogService.upcoming(AppLanguage.fromParam(lang), page);
    }

    @GetMapping("/movies/trending")
    public MovieCatalogPageDto trending(
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "week") String window) {
        return tmdbCatalogService.trending(AppLanguage.fromParam(lang), page, window);
    }

    @GetMapping("/movies/search")
    public MovieCatalogPageDto search(
            @RequestParam String q,
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbCatalogService.search(AppLanguage.fromParam(lang), page, q);
    }

    @GetMapping("/movies/{tmdbId}")
    public MovieCatalogDetailDto detail(
            @PathVariable long tmdbId,
            @RequestParam(defaultValue = "vi-VN") String lang) {
        return tmdbCatalogService.getDetail(AppLanguage.fromParam(lang), tmdbId);
    }

    @GetMapping("/genres")
    public List<TmdbGenreItemDto> genres(@RequestParam(defaultValue = "vi-VN") String lang) {
        return tmdbCatalogService.listGenres(AppLanguage.fromParam(lang));
    }

    @GetMapping("/home/bootstrap")
    public HomeBootstrapResponseDto homeBootstrap(@RequestParam(defaultValue = "vi-VN") String lang) {
        return tmdbHomeCatalogService.loadHomeBootstrap(AppLanguage.fromParam(lang));
    }

    @GetMapping("/home/now-showing")
    public HomeMoviesResponseDto homeNowShowing(
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbHomeCatalogService.loadNowShowingApi(AppLanguage.fromParam(lang), page);
    }

    @GetMapping("/home/coming-soon")
    public HomeMoviesResponseDto homeComingSoon(
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(defaultValue = "1") int page) {
        return tmdbHomeCatalogService.loadComingSoonApi(AppLanguage.fromParam(lang), page);
    }

    @GetMapping("/home/trending")
    public List<CinemaMovieCardDto> homeTrending(
            @RequestParam(defaultValue = "vi-VN") String lang,
            @RequestParam(defaultValue = "week") String window) {
        return tmdbHomeCatalogService.loadTrendingSidebar(AppLanguage.fromParam(lang), window);
    }
}
