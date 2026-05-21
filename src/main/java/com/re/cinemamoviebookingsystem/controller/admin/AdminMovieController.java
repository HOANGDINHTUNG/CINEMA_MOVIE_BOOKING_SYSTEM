package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.MovieCinemaUpdateRequest;
import com.re.cinemamoviebookingsystem.dto.response.AdminMoviePageResponse;
import com.re.cinemamoviebookingsystem.enums.AdminMovieScreeningPhase;
import com.re.cinemamoviebookingsystem.service.admin.AdminMovieCatalogService;
import com.re.cinemamoviebookingsystem.service.admin.AdminTmdbImportService;
import com.re.cinemamoviebookingsystem.service.CinemaMovieService;
import com.re.cinemamoviebookingsystem.service.MovieService;
import com.re.cinemamoviebookingsystem.service.ShowtimeScheduleService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.exception.TmdbApiException;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {

    private final MovieService movieService;
    private final AdminMovieCatalogService adminMovieCatalogService;
    private final AdminTmdbImportService adminTmdbImportService;
    private final CinemaMovieService cinemaMovieService;
    private final TmdbCatalogService tmdbCatalogService;
    private final ShowtimeScheduleService showtimeScheduleService;
    private final ShowtimeService showtimeService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "vi-VN") String lang,
                       @RequestParam(required = false) MovieStatus status,
                       @RequestParam(required = false) AdminMovieScreeningPhase phase,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        String keyword = q != null ? q : "";
        model.addAttribute("phaseCounts", adminMovieCatalogService.countByPhase(keyword, appLanguage));
        model.addAttribute("lang", lang);
        if (phase == AdminMovieScreeningPhase.INACTIVE && status == null) {
            status = MovieStatus.INACTIVE;
        }
        model.addAttribute("statusFilter", status);
        model.addAttribute("phaseFilter", phase);
        model.addAttribute("keyword", keyword);
        model.addAttribute("statuses", MovieStatus.values());
        model.addAttribute("phases", AdminMovieScreeningPhase.values());

        boolean hubView = phase == null
                || phase == AdminMovieScreeningPhase.HAS_SCHEDULE
                || phase == AdminMovieScreeningPhase.WAITING_SCHEDULE
                || phase == AdminMovieScreeningPhase.ENDED;
        model.addAttribute("hubView", hubView);

        if (hubView) {
            boolean showAll = phase == null;
            model.addAttribute("showWaitingSection", showAll || phase == AdminMovieScreeningPhase.WAITING_SCHEDULE);
            model.addAttribute("showHasScheduleSection", showAll || phase == AdminMovieScreeningPhase.HAS_SCHEDULE);
            model.addAttribute("showEndedSection", showAll || phase == AdminMovieScreeningPhase.ENDED);
            model.addAttribute("asyncHub", true);
        } else {
            var pageable = PageRequest.of(page, 15, Sort.by(Sort.Direction.DESC, "publishedAt"));
            model.addAttribute("movies",
                    adminMovieCatalogService.listForAdmin(phase, status, q, pageable, appLanguage));
        }
        return "admin/movies/list";
    }

    @GetMapping(value = "/api/section", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AdminMoviePageResponse sectionApi(@RequestParam AdminMovieScreeningPhase phase,
                                             @RequestParam(defaultValue = "vi-VN") String lang,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page) {
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        String keyword = q != null ? q : "";
        int size = phase == AdminMovieScreeningPhase.HAS_SCHEDULE ? 200 : AdminMovieCatalogService.WAITING_SCHEDULE_PAGE_SIZE;
        var result = adminMovieCatalogService.listSection(
                phase,
                keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")),
                appLanguage);
        return adminMovieCatalogService.toPageResponse(result);
    }

    @GetMapping(value = "/api/waiting-schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AdminMoviePageResponse waitingScheduleApi(@RequestParam(defaultValue = "vi-VN") String lang,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(defaultValue = "0") int page) {
        return sectionApi(AdminMovieScreeningPhase.WAITING_SCHEDULE, lang, q, page);
    }

    @GetMapping(value = "/api/posters", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<Long, String> postersApi(@RequestParam List<Long> movieIds,
                                       @RequestParam(defaultValue = "vi-VN") String lang) {
        return adminMovieCatalogService.resolvePosterUrls(movieIds, AppLanguage.fromParam(lang));
    }

    @GetMapping("/{id}/showtimes")
    public String movieShowtimes(@PathVariable Long id,
                                 @RequestParam(defaultValue = "vi-VN") String lang,
                                 Model model) {
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        var movie = movieService.findById(id, appLanguage);
        model.addAttribute("movie", movie);
        model.addAttribute("showtimes", showtimeService.listByMovieForAdmin(id));
        model.addAttribute("lang", lang);
        return "admin/movies/showtimes";
    }

    @PostMapping("/bulk-sync-tmdb")
    public String bulkSyncTmdb(@RequestParam(required = false) List<Long> movieIds,
                               @RequestParam(defaultValue = "vi-VN") String lang,
                               RedirectAttributes redirectAttributes) {
        if (movieIds == null || movieIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chọn ít nhất một phim");
            return "redirect:/admin/movies";
        }
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        int ok = 0;
        StringBuilder errors = new StringBuilder();
        for (Long id : movieIds) {
            try {
                cinemaMovieService.refreshRuntimeFromTmdb(id, appLanguage);
                ok++;
            } catch (Exception ex) {
                errors.append("#").append(id).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã sync " + ok + "/" + movieIds.size() + " phim");
        if (errors.length() > 0) {
            redirectAttributes.addFlashAttribute("errorMessage", errors.toString());
        }
        return "redirect:/admin/movies";
    }

    @GetMapping("/import")
    public String importForm(@RequestParam(required = false) String q,
                             @RequestParam(defaultValue = "vi-VN") String lang,
                             @RequestParam(defaultValue = "true") boolean onlyNew,
                             Model model) {
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        model.addAttribute("searchQuery", q != null ? q : "");
        model.addAttribute("lang", lang);
        model.addAttribute("onlyNew", onlyNew);
        try {
            model.addAttribute("hotMovies", adminTmdbImportService.listHotForImport(appLanguage, onlyNew));
        } catch (Exception ex) {
            model.addAttribute("tmdbError", ex.getMessage());
            model.addAttribute("hotMovies", List.of());
        }
        if (q != null && !q.isBlank()) {
            try {
                var searchPage = tmdbCatalogService.search(appLanguage, 1, q.trim());
                model.addAttribute("searchItems",
                        adminTmdbImportService.enrichSearchResults(searchPage, appLanguage, onlyNew));
            } catch (TmdbApiException ex) {
                model.addAttribute("tmdbError", ex.getMessage());
            }
        }
        return "admin/movies/import";
    }

    @PostMapping("/import")
    public String doPublish(@RequestParam long tmdbId,
                            @RequestParam(defaultValue = "vi-VN") String lang,
                            @RequestParam(required = false) BigDecimal defaultBasePrice,
                            @RequestParam(defaultValue = "false") boolean createSampleSchedule,
                            RedirectAttributes redirectAttributes) {
        try {
            var dto = cinemaMovieService.publishToCinema(
                    tmdbId, AppLanguage.fromParam(lang), defaultBasePrice, createSampleSchedule);
            String scheduleNote = createSampleSchedule
                    ? " (đã tạo lịch mẫu 10 ngày — mục Đã có lịch chiếu)"
                    : " — vào mục Đang đợi lịch chiếu; hãy tạo suất hoặc bấm Tạo lịch mẫu";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đăng phim TMDB #" + tmdbId + " → movieId=" + dto.getMovieId() + scheduleNote);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/movies?phase=WAITING_SCHEDULE";
    }

    @PostMapping("/{id}/sync-tmdb")
    public String syncRuntime(@PathVariable Long id,
                              @RequestParam(defaultValue = "vi-VN") String lang,
                              RedirectAttributes redirectAttributes) {
        try {
            cinemaMovieService.refreshRuntimeFromTmdb(id, AppLanguage.fromParam(lang));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật thời lượng, poster và tiêu đề từ TMDB");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/regenerate-schedule")
    public String regenerateSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String phase = "WAITING_SCHEDULE";
        try {
            int created = showtimeScheduleService.generateInitialSchedule(id);
            if (created > 0) {
                phase = "HAS_SCHEDULE";
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    created > 0 ? "Đã tạo " + created + " suất chiếu" : "Phim đã có suất — không tạo thêm");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/movies?phase=" + phase;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam(defaultValue = "vi-VN") String lang,
                           Model model) {
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        var dto = movieService.findById(id, appLanguage);
        MovieCinemaUpdateRequest req = new MovieCinemaUpdateRequest();
        req.setDuration(dto.getDuration());
        req.setAgeLabel(dto.getAgeLabel());
        req.setDefaultBasePrice(dto.getDefaultBasePrice());
        req.setAdminNote(dto.getAdminNote());
        model.addAttribute("movieRequest", req);
        model.addAttribute("movieId", id);
        model.addAttribute("tmdbId", dto.getTmdbId());
        model.addAttribute("displayTitle", dto.getDisplayTitle());
        model.addAttribute("status", dto.getStatus());
        model.addAttribute("lang", lang);
        return "admin/movies/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam(defaultValue = "vi-VN") String lang,
                         @Valid @ModelAttribute MovieCinemaUpdateRequest movieRequest,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("movieId", id);
            return "admin/movies/form";
        }
        movieService.updateCinemaFields(id, movieRequest, AppLanguage.fromParam(lang));
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin rạp");
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/delete")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            movieService.deactivate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn phim khỏi rạp (INACTIVE)");
        } catch (com.re.cinemamoviebookingsystem.exception.BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        movieService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã kích hoạt lại phim");
        return "redirect:/admin/movies";
    }
}
