package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;
import com.re.cinemamoviebookingsystem.dto.response.ShowtimeBrowseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
import com.re.cinemamoviebookingsystem.config.CinemaProperties;
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
    private final CinemaProperties cinemaProperties;

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
        model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
        return "customer/movie-detail";
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String genre,
                           @RequestParam(required = false, defaultValue = "name") String sort,
                           @RequestParam(required = false) List<String> age,
                           @RequestParam(required = false) List<String> format,
                           @RequestParam(required = false) List<String> origin,
                           @RequestParam(required = false) Boolean available,
                           @RequestParam(required = false, defaultValue = "grid") String view,
                           AppLanguage appLanguage,
                           Model model) {
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
        String viewMode = SchedulePageSupport.normalizeView(view);

        var schedule = showtimeService.buildScheduleView(date);
        var enrichedMovies = cinemaCatalogService.enrichScheduleMovies(schedule.getMovies(), appLanguage);
        enrichedMovies = TmdbGenreFilterSupport.filterSchedule(enrichedMovies, q, genre, tmdbGenres);
        enrichedMovies = SchedulePageSupport.applyExtraFilters(
                enrichedMovies, selectedAges, selectedFormats, selectedOrigins, availableOnly);
        enrichedMovies = SchedulePageSupport.sortMovies(enrichedMovies, sortKey);

        String genreParam = genre != null ? genre.trim() : "";
        boolean filtering = SchedulePageSupport.isFiltering(
                q, genreParam, selectedAges, selectedFormats, selectedOrigins, availableOnly);

        model.addAttribute("scheduleDays", schedule.getDays());
        model.addAttribute("scheduleMovies", enrichedMovies);
        model.addAttribute("selectedDate", schedule.getSelectedDate());
        model.addAttribute("tmdbGenres", tmdbGenres);
        model.addAttribute("searchQuery", q != null ? q.trim() : "");
        model.addAttribute("selectedGenre", genreParam);
        model.addAttribute("selectedGenreName", TmdbGenreFilterSupport.resolveGenreName(tmdbGenres, genreParam));
        model.addAttribute("selectedSort", sortKey);
        model.addAttribute("selectedAges", selectedAges);
        model.addAttribute("selectedFormats", selectedFormats);
        model.addAttribute("availableOnly", availableOnly);
        model.addAttribute("ageFilterOptions", SchedulePageSupport.AGE_FILTER_OPTIONS);
        model.addAttribute("formatFilterOptions", SchedulePageSupport.FORMAT_FILTER_OPTIONS);
        model.addAttribute("originFilterOptions", SchedulePageSupport.ORIGIN_FILTER_OPTIONS);
        model.addAttribute("selectedOrigins", selectedOrigins);
        model.addAttribute("viewMode", viewMode);
        model.addAttribute("resultCount", enrichedMovies.size());
        model.addAttribute("filtering", filtering);
        return "customer/calendar";
    }
}
