package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.dto.request.ShowtimeBulkCreateRequest;
import com.re.cinemamoviebookingsystem.dto.request.ShowtimeCreateRequest;
import com.re.cinemamoviebookingsystem.dto.request.ShowtimeUpdateRequest;
import com.re.cinemamoviebookingsystem.dto.response.AdminShowtimePageResponse;
import com.re.cinemamoviebookingsystem.dto.response.ShowtimeCalendarViewDto;
import com.re.cinemamoviebookingsystem.dto.response.ShowtimeConflictCheckDto;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.service.LookupService;
import com.re.cinemamoviebookingsystem.service.MovieService;
import com.re.cinemamoviebookingsystem.service.ShowtimeSeatRepairService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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
        LocalDate anchor = week != null ? week : LocalDate.now();
        LocalDate weekStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekStart.plusDays(6));
        model.addAttribute("previousWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));
        model.addAttribute("rooms", lookupService.listRooms());
        model.addAttribute("roomFilter", roomId);
        model.addAttribute("asyncCalendar", true);
        return "admin/showtimes/calendar";
    }

    @GetMapping(value = "/api/calendar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ShowtimeCalendarViewDto calendarApi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
            @RequestParam(required = false) Integer roomId) {
        return showtimeService.buildCalendarView(week, roomId);
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long movieId,
                       @RequestParam(required = false) Integer roomId,
                       @RequestParam(required = false) ShowtimeStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        try {
            showtimeService.validateAdminFilterDateRange(fromDate, toDate);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        model.addAttribute("rooms", lookupService.listRooms());
        model.addAttribute("movies", movieService.listActivePicklist());
        model.addAttribute("movieIdFilter", movieId);
        model.addAttribute("roomIdFilter", roomId);
        model.addAttribute("statusFilter", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("page", page);
        model.addAttribute("statuses", ShowtimeStatus.values());
        model.addAttribute("asyncList", true);
        model.addAttribute("todayIso", LocalDate.now());
        return "admin/showtimes/list";
    }

    @GetMapping(value = "/api/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AdminShowtimePageResponse listApi(@RequestParam(required = false) Long movieId,
                                             @RequestParam(required = false) Integer roomId,
                                             @RequestParam(required = false) ShowtimeStatus status,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                             @RequestParam(defaultValue = "0") int page) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        var pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "startTime"));
        showtimeService.validateAdminFilterDateRange(fromDate, toDate);
        var result = showtimeService.listForAdmin(movieId, roomId, status, from, to, pageable);
        return AdminShowtimePageResponse.builder()
                .items(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
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
        model.addAttribute("movies", movieService.listActivePicklist());
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
        LocalDate today = LocalDate.now();
        req.setStartDate(today);
        req.setEndDate(today);
        req.setTimeSlots("10:00,14:00,18:00,21:30");
        model.addAttribute("bulkRequest", req);
        model.addAttribute("todayIso", today);
        model.addAttribute("rooms", lookupService.listRooms());
        model.addAttribute("movies", movieService.listActivePicklist());
        return "admin/showtimes/bulk";
    }

    @PostMapping("/bulk")
    public String bulkCreate(@Valid @ModelAttribute("bulkRequest") ShowtimeBulkCreateRequest request,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("todayIso", LocalDate.now());
            model.addAttribute("rooms", lookupService.listRooms());
            model.addAttribute("movies", movieService.listActivePicklist());
            return "admin/showtimes/bulk";
        }
        try {
            var result = showtimeService.bulkCreate(request);
            if (result.getCreated() > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã tạo " + result.getCreated() + " suất chiếu");
            }
            if (result.getSkippedConflicts() > 0) {
                String preview = result.getConflictMessages().stream().limit(5)
                        .reduce((a, b) -> a + " | " + b).orElse("");
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Bỏ qua " + result.getSkippedConflicts() + " khung trùng lịch (phòng + giờ + thời lượng + "
                                + "dọn phòng). " + preview
                                + (result.getConflictMessages().size() > 5 ? " …" : ""));
            }
            if (result.getCreated() == 0 && result.getSkippedConflicts() > 0) {
                return "redirect:/admin/showtimes/bulk";
            }
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
            model.addAttribute("movies", movieService.listActivePicklist());
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
    public ShowtimeConflictCheckDto checkConflict(
            @RequestParam Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(defaultValue = "120") int durationMinutes,
            @RequestParam(required = false) Long excludeId) {
        return showtimeService.checkRoomConflictDetail(roomId, start, excludeId, durationMinutes);
    }
}
