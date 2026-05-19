package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.MovieCinemaUpdateRequest;
import com.re.cinemamoviebookingsystem.service.CinemaMovieService;
import com.re.cinemamoviebookingsystem.service.MovieService;
import com.re.cinemamoviebookingsystem.service.ShowtimeScheduleService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.exception.TmdbApiException;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/movies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {

    private final MovieService movieService;
    private final CinemaMovieService cinemaMovieService;
    private final TmdbCatalogService tmdbCatalogService;
    private final ShowtimeScheduleService showtimeScheduleService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "vi-VN") String lang, Model model) {
        AppLanguage appLanguage = AppLanguage.fromParam(lang);
        model.addAttribute("movies", movieService.listAll(PageRequest.of(0, 100), appLanguage));
        model.addAttribute("lang", lang);
        return "admin/movies/list";
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
