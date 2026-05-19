package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;
import com.re.cinemamoviebookingsystem.dto.response.ShowtimeBrowseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
import com.re.cinemamoviebookingsystem.service.CinemaCatalogService;
import com.re.cinemamoviebookingsystem.service.CinemaMovieService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerMovieController {

    private final ShowtimeService showtimeService;
    private final CinemaCatalogService cinemaCatalogService;
    private final CinemaMovieService cinemaMovieService;
    private final TmdbCatalogService tmdbCatalogService;

    @GetMapping("/movies/{tmdbId}")
    public String movieDetail(@PathVariable long tmdbId,
                              AppLanguage appLanguage,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        MovieCatalogDetailDto detail;
        try {
            detail = tmdbCatalogService.getDetail(appLanguage, tmdbId);
        } catch (Exception ex) {
            log.warn("TMDB detail failed for tmdbId={}: {}", tmdbId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không tải được thông tin phim. Vui lòng thử lại sau.");
            return "redirect:/customer/home";
        }

        boolean published = cinemaMovieService.isPublishedAndActive(tmdbId);
        List<ShowtimeBrowseDto> showtimes = published
                ? showtimeService.listByTmdbId(tmdbId)
                : List.of();

        model.addAttribute("catalogDetail", detail);
        model.addAttribute("tmdbId", tmdbId);
        model.addAttribute("publishedAtCinema", published);
        model.addAttribute("showtimes", showtimes);
        model.addAttribute("showtimeDays", showtimeService.groupShowtimesByDay(showtimes, appLanguage.toLocale()));
        return "customer/movie-detail";
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String genre,
                           AppLanguage appLanguage,
                           Model model) {
        List<TmdbGenreItemDto> tmdbGenres = List.of();
        try {
            tmdbGenres = tmdbCatalogService.listGenres(appLanguage);
        } catch (Exception ignored) {
        }
        var schedule = showtimeService.buildScheduleView(date);
        var enrichedMovies = cinemaCatalogService.enrichScheduleMovies(schedule.getMovies(), appLanguage);
        enrichedMovies = TmdbGenreFilterSupport.filterSchedule(enrichedMovies, q, genre, tmdbGenres);
        boolean filtering = (q != null && !q.isBlank()) || (genre != null && !genre.isBlank());
        model.addAttribute("scheduleDays", schedule.getDays());
        model.addAttribute("scheduleMovies", enrichedMovies);
        model.addAttribute("selectedDate", schedule.getSelectedDate());
        model.addAttribute("tmdbGenres", tmdbGenres);
        model.addAttribute("searchQuery", q != null ? q.trim() : "");
        model.addAttribute("selectedGenre", genre != null ? genre.trim() : "");
        model.addAttribute("filtering", filtering);
        return "customer/calendar";
    }
}
