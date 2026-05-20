package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.response.catalog.CinemaMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.HeroSlideDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.HomeBootstrapResponseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.HomeMoviesResponseDto;
import com.re.cinemamoviebookingsystem.service.TmdbHomeCatalogService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

/**
 * Trang chủ: phim từ DB (có suất); metadata TMDB; sidebar xu hướng lọc theo rạp.
 */
@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerHomeController {

    private final CinemaProperties cinemaProperties;
    private final TmdbHomeCatalogService tmdbHomeCatalogService;

    @GetMapping("/home")
    public String home(AppLanguage appLanguage, Model model) {
        HomeBootstrapResponseDto bundle = tmdbHomeCatalogService.loadHomeBootstrap(appLanguage);

        HomeMoviesResponseDto now = bundle.getNowShowing();
        HomeMoviesResponseDto soon = bundle.getComingSoon();
        List<CinemaMovieCardDto> nowMovies = filterRenderableCards(
                now != null && now.getMovies() != null ? now.getMovies() : Collections.emptyList());
        List<CinemaMovieCardDto> soonMovies = filterRenderableCards(
                soon != null && soon.getMovies() != null ? soon.getMovies() : Collections.emptyList());
        List<CinemaMovieCardDto> trending = filterRenderableCards(
                bundle.getTrending() != null ? bundle.getTrending() : Collections.emptyList());
        List<HeroSlideDto> heroSlides = bundle.getHeroSlides() != null
                ? bundle.getHeroSlides() : Collections.emptyList();

        model.addAttribute("appLang", appLanguage.getTmdbCode());
        model.addAttribute("heroSlides", heroSlides);
        model.addAttribute("nowShowing", nowMovies);
        model.addAttribute("comingSoon", soonMovies);
        model.addAttribute("trending", trending);
        model.addAttribute("trendingWindow", TmdbHomeCatalogService.TRENDING_WINDOW);
        model.addAttribute("nowHasMore", now != null && now.isHasMore());
        model.addAttribute("soonHasMore", soon != null && soon.isHasMore());
        model.addAttribute("homeError", bundle.getError());
        model.addAttribute("homePageSize", TmdbHomeCatalogService.PAGE_SIZE);
        model.addAttribute("maxSeatsPerBooking", cinemaProperties.getMaxSeatsPerBooking());
        return "customer/home";
    }

    private static List<CinemaMovieCardDto> filterRenderableCards(List<CinemaMovieCardDto> cards) {
        return cards.stream()
                .filter(c -> c != null && c.getTmdbId() != null)
                .toList();
    }
}
