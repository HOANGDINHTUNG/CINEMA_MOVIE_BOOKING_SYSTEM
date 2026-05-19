package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.request.CheckoutRequest;
import com.re.cinemamoviebookingsystem.enums.PaymentMode;
import com.re.cinemamoviebookingsystem.repository.ComboRepository;
import com.re.cinemamoviebookingsystem.service.BookingHistoryService;
import com.re.cinemamoviebookingsystem.service.BookingService;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.service.VnPaySandboxService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.util.SeatLayoutHelper;
import com.re.cinemamoviebookingsystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
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

    @GetMapping("/showtimes/{id}/seats")
    public String seatMap(@PathVariable Long id, AppLanguage appLanguage, Model model) {
        var seatMap = showtimeService.getSeatMap(id);
        model.addAttribute("seatMap", seatMap);
        model.addAttribute("seatsByRow", SeatLayoutHelper.groupByRow(seatMap.getSeats()));
        model.addAttribute("vipMultiplier", cinemaProperties.getVipPriceMultiplier());
        model.addAttribute("maxSeatsPerBooking", cinemaProperties.getMaxSeatsPerBooking());
        model.addAttribute("seatLockMinutes", cinemaProperties.getSeatLockMinutes());
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

    @PostMapping("/showtimes/{id}/lock")
    public String lockSeats(@PathVariable Long id,
                            @RequestParam(required = false) List<Long> seatIds,
                            RedirectAttributes redirectAttributes) {
        if (seatIds == null || seatIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ít nhất một ghế.");
            return "redirect:/customer/showtimes/" + id + "/seats";
        }
        if (seatIds.size() > cinemaProperties.getMaxSeatsPerBooking()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Mỗi đơn chỉ được đặt tối đa " + cinemaProperties.getMaxSeatsPerBooking() + " ghế.");
            return "redirect:/customer/showtimes/" + id + "/seats";
        }
        try {
            bookingService.lockSeats(id, seatIds, SecurityUtils.currentUserId());
            redirectAttributes.addFlashAttribute("seatIds", seatIds);
            return "redirect:/customer/showtimes/" + id + "/checkout";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/showtimes/" + id + "/seats";
        }
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
        var seatMap = showtimeService.getSeatMap(showtimeId);
        var seatLabels = seatMap.getSeats().stream()
                .filter(s -> seatIds.contains(s.getSeatId()))
                .map(com.re.cinemamoviebookingsystem.dto.response.SeatMapDto.SeatCellDto::getLabel)
                .toList();
        model.addAttribute("seatMap", seatMap);
        model.addAttribute("seatIds", seatIds);
        model.addAttribute("seatLabels", seatLabels);
        model.addAttribute("estimatedTotal", showtimeService.estimateSeatTotal(seatMap, seatIds));
        model.addAttribute("combos", comboRepository.findByStatus("ACTIVE"));
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
    public String history(Model model) {
        model.addAttribute("bookings", bookingHistoryService.getHistoryForUser(SecurityUtils.currentUserId()));
        return "customer/bookings";
    }

    @GetMapping("/bookings/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("booking", bookingHistoryService.getBookingDetail(id, SecurityUtils.currentUserId()));
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
