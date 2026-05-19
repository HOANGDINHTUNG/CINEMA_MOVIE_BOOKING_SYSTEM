package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.dto.response.catalog.CinemaMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;
import com.re.cinemamoviebookingsystem.service.CinemaCatalogService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerCatalogController {

    private final TmdbCatalogService tmdbCatalogService;
    private final CinemaCatalogService cinemaCatalogService;

    private static final int CATALOG_MOVIE_LIMIT = 100;

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) String q,
                          @RequestParam(required = false) String genre,
                          AppLanguage appLanguage,
                          Model model) {
        String tmdbError = null;
        List<CinemaMovieCardDto> catalogCards = List.of();
        List<TmdbGenreItemDto> tmdbGenres = List.of();
        try {
            tmdbGenres = tmdbCatalogService.listGenres(appLanguage);
            Integer genreId = TmdbGenreFilterSupport.parseGenreId(genre);
            var summaries = tmdbCatalogService.discoverLatestUpTo(appLanguage, CATALOG_MOVIE_LIMIT, genreId);
            var genreMap = tmdbCatalogService.genreNameMap(appLanguage);
            catalogCards = cinemaCatalogService.mapSummariesToCards(summaries, genreMap);
            catalogCards = TmdbGenreFilterSupport.filterByTitle(catalogCards, q);
        } catch (Exception ex) {
            tmdbError = ex.getMessage();
        }
        model.addAttribute("catalogCards", catalogCards);
        model.addAttribute("tmdbGenres", tmdbGenres);
        model.addAttribute("tmdbError", tmdbError);
        model.addAttribute("searchQuery", q != null ? q.trim() : "");
        model.addAttribute("selectedGenre", genre != null ? genre.trim() : "");
        model.addAttribute("catalogLimit", CATALOG_MOVIE_LIMIT);
        return "customer/catalog";
    }

    @GetMapping("/catalog/movie/{tmdbId}")
    public String catalogMovieRedirect(@PathVariable long tmdbId) {
        return "redirect:/customer/movies/" + tmdbId;
    }
}
