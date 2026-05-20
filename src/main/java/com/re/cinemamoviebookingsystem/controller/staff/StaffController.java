package com.re.cinemamoviebookingsystem.controller.staff;

import com.re.cinemamoviebookingsystem.service.StaffBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffBookingService staffBookingService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "staff/dashboard";
    }

    @GetMapping("/lookup")
    public String lookupForm(@RequestParam(required = false) Long bookingId,
                             @RequestParam(required = false) String ticketCode,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bookingId == null && (ticketCode == null || ticketCode.isBlank())) {
            return "staff/lookup";
        }
        try {
            if (bookingId != null) {
                model.addAttribute("booking", staffBookingService.findByBookingId(bookingId));
            } else {
                model.addAttribute("booking", staffBookingService.findByTicketCode(ticketCode.trim()));
            }
            return "staff/lookup-result";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/staff/lookup";
        }
    }

    @PostMapping("/lookup")
    public String lookup(@RequestParam(required = false) Long bookingId,
                         @RequestParam(required = false) String ticketCode,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            if (bookingId != null) {
                model.addAttribute("booking", staffBookingService.findByBookingId(bookingId));
            } else if (ticketCode != null && !ticketCode.isBlank()) {
                model.addAttribute("booking", staffBookingService.findByTicketCode(ticketCode.trim()));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Nhập mã đơn hoặc mã vé");
                return "redirect:/staff/lookup";
            }
            return "staff/lookup-result";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/staff/lookup";
        }
    }

    @PostMapping("/bookings/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            staffBookingService.confirmAtCounter(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận thanh toán thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff/lookup?bookingId=" + id;
    }

    @GetMapping("/bookings/{id}/print")
    public String print(@PathVariable Long id, Model model) {
        model.addAttribute("booking", staffBookingService.findByBookingId(id));
        return "staff/print-ticket";
    }
}
