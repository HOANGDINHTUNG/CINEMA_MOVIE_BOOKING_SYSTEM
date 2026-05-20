package com.re.cinemamoviebookingsystem.controller.admin;

import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.service.AdminBookingService;
import com.re.cinemamoviebookingsystem.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final AuditLogService auditLogService;

    @GetMapping
    public String list(@RequestParam(required = false) BookingStatus status,
                       @RequestParam(required = false) Long bookingId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "bookingDate"));
        model.addAttribute("bookings", adminBookingService.list(status, bookingId, pageable));
        model.addAttribute("statusFilter", status);
        model.addAttribute("bookingIdFilter", bookingId);
        model.addAttribute("statuses", BookingStatus.values());
        return "admin/bookings/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("booking", adminBookingService.detail(id));
        return "admin/bookings/detail";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.confirmAtCounter(id);
            auditLogService.log("BOOKING_CONFIRM_COUNTER", "BOOKING", String.valueOf(id), null);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận thanh toán quầy");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    @PostMapping("/{id}/cancel-pending")
    public String cancelPending(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.cancelPending(id);
            auditLogService.log("BOOKING_CANCEL_PENDING", "BOOKING", String.valueOf(id), null);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn chờ thanh toán");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    @PostMapping("/{id}/cancel-paid")
    public String cancelPaid(@PathVariable Long id,
                             @RequestParam(defaultValue = "false") boolean force,
                             RedirectAttributes redirectAttributes) {
        try {
            adminBookingService.cancelPaid(id, force);
            auditLogService.log("BOOKING_CANCEL_PAID", "BOOKING", String.valueOf(id), "force=" + force);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn đã thanh toán");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }
}
