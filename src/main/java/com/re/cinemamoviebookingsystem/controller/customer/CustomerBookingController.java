package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.security.BookingResumeSession;
import com.re.cinemamoviebookingsystem.security.LoginAuthenticationSuccessHandler;
import com.re.cinemamoviebookingsystem.dto.request.CheckoutRequest;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.enums.PaymentMode;
import com.re.cinemamoviebookingsystem.repository.ComboRepository;
import com.re.cinemamoviebookingsystem.service.BookingHistoryService;
import com.re.cinemamoviebookingsystem.service.BookingService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.service.VnPaySandboxService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.util.SeatLayoutHelper;
import com.re.cinemamoviebookingsystem.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerBookingController {

    private final ShowtimeService showtimeService;
    private final BookingService bookingService;
    private final BookingHistoryService bookingHistoryService;
    private final ComboRepository comboRepository;
    private final VnPaySandboxService vnPaySandboxService;
    private final CinemaProperties cinemaProperties;
    private final MessageSource messageSource;

    @GetMapping("/showtimes/{id}/seats")
    public String seatMap(@PathVariable Long id, AppLanguage appLanguage, Model model, HttpServletRequest request) {
        ensureCsrfInitialized(request);
        var seatMap = showtimeService.getSeatMap(id, SecurityUtils.optionalCurrentUserId());
        model.addAttribute("seatMap", seatMap);
        model.addAttribute("seatsByRow", SeatLayoutHelper.groupByRow(seatMap.getSeats()));
        model.addAttribute("vipMultiplier", cinemaProperties.getVipPriceMultiplier());
        model.addAttribute("maxSeatsPerBooking", cinemaProperties.getMaxSeatsPerBooking());
        model.addAttribute("seatLockMinutes", cinemaProperties.getSeatLockMinutes());
        model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
        model.addAttribute("seatsConfigured", seatMap.getSeats() != null && !seatMap.getSeats().isEmpty());
        Long userId = SecurityUtils.optionalCurrentUserId();
        boolean seatsLockedOnServer = bookingService.hasActiveSeatLock(id, userId);
        model.addAttribute("seatSyncEnabled", userId != null);
        model.addAttribute("seatsLockedOnServer", seatsLockedOnServer);
        var selectedDate = seatMap.getStartTime().toLocalDate();
        model.addAttribute("selectedShowtimeDate", selectedDate);
        if (seatMap.getTmdbId() != null) {
            var showtimes = showtimeService.listByTmdbId(seatMap.getTmdbId());
            var showtimeDays = showtimeService.groupShowtimesByDay(showtimes, appLanguage.toLocale());
            model.addAttribute("showtimeDays", showtimeDays);
            var currentDaySlots = showtimeDays.stream()
                    .filter(d -> d.getDate().equals(selectedDate))
                    .findFirst()
                    .map(d -> d.getSlots())
                    .orElse(List.of());
            model.addAttribute("currentDaySlots", currentDaySlots);
        }
        return "customer/seats";
    }

    private void ensureCsrfInitialized(HttpServletRequest request) {
        Object attr = request.getAttribute("_csrf");
        if (attr instanceof CsrfToken token) {
            token.getToken();
        }
        request.getSession(true);
    }

    /**
     * Bắt đầu đặt vé khi chưa đăng nhập: lưu ghế vào session và chuyển login,
     * sau đăng nhập quay lại {@link #resumeBooking(HttpServletRequest)}.
     */
    @PostMapping("/booking/start")
    public String startBooking(@RequestParam("showtimeId") Long showtimeId,
                               @RequestParam(value = "seatIds", required = false) List<Long> seatIds,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        var locale = LocaleContextHolder.getLocale();
        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("seat.pick_at_least", null, locale));
            return "redirect:/customer/showtimes/" + showtimeId + "/seats";
        }
        if (seatIds.size() > cinemaProperties.getMaxSeatsPerBooking()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("booking.error.max_seats",
                            new Object[]{cinemaProperties.getMaxSeatsPerBooking()}, locale));
            return "redirect:/customer/showtimes/" + showtimeId + "/seats";
        }

        if (SecurityUtils.optionalCurrentUserId() == null) {
            BookingResumeSession.save(request, showtimeId, seatIds);
            String loginUrl = request.getContextPath() + "/login?redirect="
                    + LoginAuthenticationSuccessHandler.encodeContinueUrl(showtimeId);
            return "redirect:" + loginUrl;
        }

        try {
            bookingService.lockSeats(showtimeId, seatIds, SecurityUtils.currentUserId());
            redirectAttributes.addFlashAttribute("seatIds", seatIds);
            return "redirect:/customer/showtimes/" + showtimeId + "/checkout";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/showtimes/" + showtimeId + "/seats";
        }
    }

    @GetMapping("/booking/continue")
    public String resumeBooking(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Long showtimeId = BookingResumeSession.getShowtimeId(request);
        List<Long> seatIds = BookingResumeSession.getSeatIds(request);
        BookingResumeSession.clear(request);

        if (showtimeId == null || seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên đặt vé đã hết hạn. Vui lòng chọn ghế lại.");
            return "redirect:/customer/home";
        }

        try {
            bookingService.lockSeats(showtimeId, seatIds, SecurityUtils.currentUserId());
            redirectAttributes.addFlashAttribute("seatIds", seatIds);
            return "redirect:/customer/showtimes/" + showtimeId + "/checkout";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/showtimes/" + showtimeId + "/seats";
        }
    }

    @PostMapping("/showtimes/{id}/seats/sync-lock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> syncSeatLock(@PathVariable Long id,
                                                            @RequestParam(value = "seatIds", required = false) List<Long> seatIds) {
        try {
            bookingService.syncSeatLocks(id, seatIds != null ? seatIds : List.of(), SecurityUtils.currentUserId());
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/showtimes/{id}/seats/release-lock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> releaseSeatLock(@PathVariable Long id) {
        try {
            bookingService.releaseAllSeatLocks(id, SecurityUtils.currentUserId());
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/showtimes/{id}/lock")
    public String lockSeats(@PathVariable Long id,
                            @RequestParam(required = false) List<Long> seatIds,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        return startBooking(id, seatIds, request, redirectAttributes);
    }

    @GetMapping("/showtimes/{id}/checkout")
    public String checkoutForm(@PathVariable Long id,
                               @RequestParam(required = false) List<Long> seatIds,
                               Model model) {
        if (seatIds == null || seatIds.isEmpty()) {
            Object flashSeatIds = model.asMap().get("seatIds");
            if (flashSeatIds instanceof List<?> list) {
                seatIds = list.stream()
                        .filter(Long.class::isInstance)
                        .map(Long.class::cast)
                        .toList();
            }
        }
        if (seatIds == null || seatIds.isEmpty()) {
            return "redirect:/customer/showtimes/" + id + "/seats";
        }
        addCheckoutModel(id, seatIds, model);
        CheckoutRequest req = new CheckoutRequest();
        req.setShowtimeId(id);
        req.setSeatIds(seatIds);
        req.setPaymentMode(PaymentMode.ONLINE);
        model.addAttribute("checkoutRequest", req);
        return "customer/checkout";
    }

    private void addCheckoutModel(Long showtimeId, List<Long> seatIds, Model model) {
        var seatMap = showtimeService.getSeatMap(showtimeId, SecurityUtils.currentUserId());
        var seatLabels = seatMap.getSeats().stream()
                .filter(s -> seatIds.contains(s.getSeatId()))
                .map(com.re.cinemamoviebookingsystem.dto.response.SeatMapDto.SeatCellDto::getLabel)
                .toList();
        var estimatedTotal = showtimeService.estimateSeatTotal(seatMap, seatIds);
        model.addAttribute("seatMap", seatMap);
        model.addAttribute("seatIds", seatIds);
        model.addAttribute("seatLabels", seatLabels);
        model.addAttribute("seatLabelsJoined", String.join(", ", seatLabels));
        model.addAttribute("estimatedTotal", estimatedTotal);
        model.addAttribute("formattedEstimatedTotal", formatVnd(estimatedTotal));
        if (seatMap.getLockExpiresAt() != null) {
            model.addAttribute("lockExpiresAtIso",
                    seatMap.getLockExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        model.addAttribute("combos", comboRepository.findByStatus("ACTIVE"));
        model.addAttribute("seatLockMinutes", cinemaProperties.getSeatLockMinutes());
        model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
        model.addAttribute("showtimeTime", seatMap.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        model.addAttribute("showtimeDate", seatMap.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute CheckoutRequest checkoutRequest,
                           BindingResult bindingResult,
                           @RequestParam(required = false) Map<String, String> comboQty,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "redirect:/customer/showtimes/" + checkoutRequest.getShowtimeId() + "/seats";
        }
        checkoutRequest.setComboQuantities(parseCombos(comboQty));
        try {
            Long bookingId = bookingService.checkout(checkoutRequest, SecurityUtils.currentUserId());
            if (checkoutRequest.getPaymentMode() == PaymentMode.ONLINE) {
                redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công!");
                return "redirect:/customer/bookings/" + bookingId;
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt vé thành công. Vui lòng thanh toán tại quầy trong 15 phút. Mã đơn: " + bookingId);
            return "redirect:/customer/bookings/" + bookingId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/showtimes/" + checkoutRequest.getShowtimeId() + "/seats";
        }
    }

    @GetMapping("/bookings")
    public String history(@RequestParam(value = "status", required = false) String status, Model model) {
        var all = bookingHistoryService.getHistoryForUser(SecurityUtils.currentUserId());
        if (status != null && !status.isBlank()) {
            String filter = status.trim().toUpperCase();
            all = all.stream()
                    .filter(b -> b.getStatus().name().equals(filter))
                    .toList();
        }
        model.addAttribute("bookings", all);
        model.addAttribute("statusFilter", status != null ? status.trim().toUpperCase() : "ALL");
        model.addAttribute("accountSection", "bookings");
        model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
        return "customer/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        var booking = bookingHistoryService.getBookingDetail(id, SecurityUtils.currentUserId());
        if (booking.getStatus() == BookingStatus.HELD) {
            if (!booking.isHeldActive()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Phiên giữ ghế đã hết hạn. Vui lòng chọn ghế lại.");
            }
            return "redirect:/customer/showtimes/" + booking.getShowtimeId() + "/seats";
        }
        model.addAttribute("booking", booking);
        model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
        return "customer/booking-detail";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(id, SecurityUtils.currentUserId());
            redirectAttributes.addFlashAttribute("successMessage", "Hủy vé thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/bookings";
    }

    @PostMapping("/bookings/{id}/pay-vnpay")
    public String payVnPay(@PathVariable Long id) {
        String url = vnPaySandboxService.createPaymentUrl(id);
        return "redirect:" + url;
    }

    private static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0đ";
        }
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(amount) + "đ";
    }

    private Map<Integer, Integer> parseCombos(Map<String, String> comboQty) {
        Map<Integer, Integer> map = new HashMap<>();
        if (comboQty == null) return map;
        comboQty.forEach((k, v) -> {
            if (k.startsWith("combo_") && v != null && !v.isBlank()) {
                try {
                    int qty = Integer.parseInt(v);
                    if (qty > 0) {
                        map.put(Integer.parseInt(k.replace("combo_", "")), qty);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });
        return map;
    }
}
