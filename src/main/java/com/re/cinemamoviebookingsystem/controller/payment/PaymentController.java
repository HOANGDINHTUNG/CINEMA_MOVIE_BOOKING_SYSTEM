package com.re.cinemamoviebookingsystem.controller.payment;

import com.re.cinemamoviebookingsystem.service.VnPaySandboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final VnPaySandboxService vnPaySandboxService;

    @GetMapping("/payment/vnpay-return")
    public String vnpayReturn(@RequestParam Long bookingId,
                              @RequestParam String txnId,
                              @RequestParam String status,
                              RedirectAttributes redirectAttributes) {
        try {
            vnPaySandboxService.handleReturn(bookingId, txnId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán VNPay thành công");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/bookings/" + bookingId;
    }
}
