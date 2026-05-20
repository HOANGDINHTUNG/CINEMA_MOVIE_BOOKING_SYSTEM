package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.MovieCinemaUpdateRequest;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {

    private final MovieService movieService;
    private final CinemaMovieService cinemaMovieService;
    private final TmdbCatalogService tmdbCatalogService;
    private final ShowtimeScheduleService showtimeScheduleService;
    private final ShowtimeService showtimeService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "vi-VN") String lang,
                       @RequestParam(required = false) MovieStatus status,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        var pageable = PageRequest.of(page, 15, Sort.by(Sort.Direction.DESC, "publishedAt"));
        model.addAttribute("movies", movieService.listForAdmin(status, q, pageable, appLanguage));
        model.addAttribute("lang", lang);
        model.addAttribute("statusFilter", status);
        model.addAttribute("keyword", q != null ? q : "");
        model.addAttribute("statuses", MovieStatus.values());
        return "admin/movies/list";
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
                             Model model) {
        model.addAttribute("searchQuery", q != null ? q : "");
        model.addAttribute("lang", lang);
        if (q != null && !q.isBlank()) {
            try {
                model.addAttribute("searchResults",
                        tmdbCatalogService.search(AppLanguage.fromParam(lang), 1, q.trim()));
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
                            RedirectAttributes redirectAttributes) {
        try {
            var dto = cinemaMovieService.publishToCinema(tmdbId, AppLanguage.fromParam(lang), defaultBasePrice);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đăng chiếu TMDB #" + tmdbId + " → movieId=" + dto.getMovieId()
                            + " (đã tạo lịch chiếu mẫu 10 ngày)");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/sync-tmdb")
    public String syncRuntime(@PathVariable Long id,
                              @RequestParam(defaultValue = "vi-VN") String lang,
                              RedirectAttributes redirectAttributes) {
        try {
            cinemaMovieService.refreshRuntimeFromTmdb(id, AppLanguage.fromParam(lang));
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thời lượng / độ tuổi từ TMDB");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/regenerate-schedule")
    public String regenerateSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            int created = showtimeScheduleService.generateInitialSchedule(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    created > 0 ? "Đã tạo " + created + " suất chiếu" : "Phim đã có suất — không tạo thêm");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/movies/" + id + "/edit";
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
        movieService.deactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn phim khỏi rạp (INACTIVE)");
        return "redirect:/admin/movies";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        movieService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã kích hoạt lại phim");
        return "redirect:/admin/movies";
    }
}
