package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.ShowtimeBulkCreateRequest;
import com.re.cinemamoviebookingsystem.dto.request.ShowtimeCreateRequest;
import com.re.cinemamoviebookingsystem.dto.request.ShowtimeUpdateRequest;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.service.LookupService;
import com.re.cinemamoviebookingsystem.service.MovieService;
import com.re.cinemamoviebookingsystem.service.ShowtimeSeatRepairService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/showtimes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;
    private final LookupService lookupService;
    private final MovieService movieService;
    private final ShowtimeSeatRepairService showtimeSeatRepairService;

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                           @RequestParam(required = false) Integer roomId,
                           Model model) {
        model.addAttribute("calendar", showtimeService.buildCalendarView(week, roomId));
        model.addAttribute("rooms", lookupService.listRooms());
        model.addAttribute("roomFilter", roomId);
        return "admin/showtimes/calendar";
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long movieId,
                       @RequestParam(required = false) Integer roomId,
                       @RequestParam(required = false) ShowtimeStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        var pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "startTime"));
        model.addAttribute("showtimes", showtimeService.listForAdmin(movieId, roomId, status, from, to, pageable));
        model.addAttribute("rooms", lookupService.listRooms());
        model.addAttribute("movies", movieService.listActive(AppLanguage.VI_VN));
        model.addAttribute("movieIdFilter", movieId);
        model.addAttribute("roomIdFilter", roomId);
        model.addAttribute("statusFilter", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", ShowtimeStatus.values());
        return "admin/showtimes/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("showtime", showtimeService.getAdminDetail(id));
        return "admin/showtimes/detail";
    }

    @GetMapping("/new")
    public String form(@RequestParam(required = false) Integer roomId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                       Model model) {
        ShowtimeCreateRequest req = new ShowtimeCreateRequest();
        if (roomId != null) {
            req.setRoomId(roomId);
        }
        if (startTime != null) {
            req.setStartTime(startTime);
        }
        model.addAttribute("showtimeRequest", req);
        model.addAttribute("rooms", lookupService.listRooms());
        model.addAttribute("movies", movieService.listActive(AppLanguage.VI_VN));
        return "admin/showtimes/form";
    }

    @GetMapping("/bulk")
    public String bulkForm(@RequestParam(required = false) Long movieId, Model model) {
        ShowtimeBulkCreateRequest req = new ShowtimeBulkCreateRequest();
        if (movieId != null) {
            req.setMovieId(movieId);
            var movie = movieService.findById(movieId);
            req.setTmdbId(movie.getTmdbId());
            if (movie.getDefaultBasePrice() != null) {
                req.setBasePrice(movie.getDefaultBasePrice());
            }
        }
        req.setTimeSlots("10:00,14:00,18:00,21:30");
        model.addAttribute("bulkRequest", req);
        model.addAttribute("rooms", lookupService.listRooms());
        model.addAttribute("movies", movieService.listActive(AppLanguage.VI_VN));
        return "admin/showtimes/bulk";
    }

    @PostMapping("/bulk")
    public String bulkCreate(@Valid @ModelAttribute("bulkRequest") ShowtimeBulkCreateRequest request,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", lookupService.listRooms());
            model.addAttribute("movies", movieService.listActive(AppLanguage.VI_VN));
            return "admin/showtimes/bulk";
        }
        try {
            int created = showtimeService.bulkCreate(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo " + created + " suất chiếu");
            return "redirect:/admin/showtimes";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/showtimes/bulk";
        }
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("showtimeRequest") ShowtimeCreateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", lookupService.listRooms());
            model.addAttribute("movies", movieService.listActive(AppLanguage.VI_VN));
            return "admin/showtimes/form";
        }
        try {
            showtimeService.createShowtime(request);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo suất chiếu thành công");
            return "redirect:/admin/showtimes";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/showtimes/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var detail = showtimeService.getAdminDetail(id);
        ShowtimeUpdateRequest req = new ShowtimeUpdateRequest();
        req.setRoomId(detail.getRoomId());
        req.setStartTime(detail.getStartTime());
        req.setBasePrice(detail.getBasePrice());
        model.addAttribute("showtimeRequest", req);
        model.addAttribute("showtime", detail);
        model.addAttribute("rooms", lookupService.listRooms());
        return "admin/showtimes/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("showtimeRequest") ShowtimeUpdateRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("showtime", showtimeService.getAdminDetail(id));
            model.addAttribute("rooms", lookupService.listRooms());
            return "admin/showtimes/edit";
        }
        try {
            showtimeService.updateShowtime(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật suất chiếu");
            return "redirect:/admin/showtimes/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/showtimes/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/repair-seats")
    public String repairSeats(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        int added = showtimeSeatRepairService.rebuildShowtimeSeats(id);
        if (added > 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã tạo " + added + " dòng ghế cho suất chiếu.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Suất đã có sơ đồ ghế hoặc phòng chưa có ghế trong DB.");
        }
        return "redirect:/admin/showtimes/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            showtimeService.cancelShowtime(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy suất chiếu");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/showtimes/" + id;
    }

    @GetMapping("/check-conflict")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkConflict(
            @RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(defaultValue = "120") int durationMinutes,
            @RequestParam(required = false) Long excludeId) {
        boolean conflict = showtimeService.checkRoomConflict(roomId, start, excludeId, durationMinutes);
        return ResponseEntity.ok(Map.of("conflict", conflict));
    }
}
