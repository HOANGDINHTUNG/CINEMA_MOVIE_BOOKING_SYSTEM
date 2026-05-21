package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.dto.response.ShowtimeBrowseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.service.CinemaCatalogService;
import com.re.cinemamoviebookingsystem.service.CinemaMovieService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import com.re.cinemamoviebookingsystem.util.CountryNames;
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
import java.util.Locale;

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

    /**
     * Shell lịch chiếu — dữ liệu suất/TMDB tải qua {@code GET /api/public/schedule}.
     */
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
                           Locale locale,
                           Model model) {
        String sortKey = SchedulePageSupport.normalizeSort(sort);
        List<String> selectedAges = SchedulePageSupport.normalizeAgeFilters(age);
        List<String> selectedFormats = SchedulePageSupport.normalizeFormatFilters(format);
        List<String> selectedOrigins = SchedulePageSupport.normalizeOriginFilters(origin);
        boolean availableOnly = Boolean.TRUE.equals(available);
        String viewMode = SchedulePageSupport.normalizeView(view);
        String genreParam = genre != null ? genre.trim() : "";
        LocalDate selectedDate = date != null ? date : LocalDate.now();

        model.addAttribute("appLang", appLanguage.getTmdbCode());
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("searchQuery", q != null ? q.trim() : "");
        model.addAttribute("selectedGenre", genreParam);
        model.addAttribute("selectedSort", sortKey);
        model.addAttribute("selectedAges", selectedAges);
        model.addAttribute("selectedFormats", selectedFormats);
        model.addAttribute("availableOnly", availableOnly);
        model.addAttribute("ageFilterOptions", SchedulePageSupport.AGE_FILTER_OPTIONS);
        model.addAttribute("formatFilterOptions", SchedulePageSupport.FORMAT_FILTER_OPTIONS);
        model.addAttribute("originFilterOptions", SchedulePageSupport.ORIGIN_FILTER_OPTIONS);
        model.addAttribute("selectedOrigins", selectedOrigins);
        model.addAttribute("viewMode", viewMode);
        model.addAttribute("countryLabelsJson", CountryNames.labelsJsonForLocale(locale));
        return "customer/calendar";
    }
}
