package com.re.cinemamoviebookingsystem.controller.customer;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.response.VoucherPreviewDto;
import com.re.cinemamoviebookingsystem.entity.Combo;
import com.re.cinemamoviebookingsystem.repository.ComboRepository;
import com.re.cinemamoviebookingsystem.service.ShowtimeService;
import com.re.cinemamoviebookingsystem.service.VoucherService;
import com.re.cinemamoviebookingsystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CustomerVoucherController {

    private final VoucherService voucherService;
    private final ShowtimeService showtimeService;
    private final ComboRepository comboRepository;
    private final CinemaProperties cinemaProperties;

    @GetMapping("/customer/account/vouchers")
    public String myVouchers(Model model) {
        model.addAttribute("vouchers", voucherService.getWallet(SecurityUtils.currentUserId()));
        model.addAttribute("accountSection", "vouchers");
        model.addAttribute("cinemaBrandName", cinemaProperties.getBrandName());
        return "customer/account/vouchers";
    }

    @GetMapping("/customer/checkout/voucher-preview")
    @ResponseBody
    public ResponseEntity<VoucherPreviewDto> voucherPreview(
            @RequestParam Long showtimeId,
            @RequestParam List<Long> seatIds,
            @RequestParam(required = false) Long userVoucherId,
            @RequestParam(required = false) Map<String, String> comboQty) {
        Long userId = SecurityUtils.currentUserId();
        var seatMap = showtimeService.getSeatMap(showtimeId, userId);
        BigDecimal ticketTotal = showtimeService.estimateSeatTotal(seatMap, seatIds);
        BigDecimal comboTotal = parseComboTotal(comboQty);
        BigDecimal subtotal = ticketTotal.add(comboTotal);
        boolean hasCombo = comboTotal.compareTo(BigDecimal.ZERO) > 0;
        return ResponseEntity.ok(voucherService.preview(userId, userVoucherId, subtotal, hasCombo));
    }

    private BigDecimal parseComboTotal(Map<String, String> comboQty) {
        if (comboQty == null || comboQty.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Integer, Integer> map = new HashMap<>();
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
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            Combo combo = comboRepository.findById(e.getKey()).orElse(null);
            if (combo != null && e.getValue() != null && e.getValue() > 0) {
                total = total.add(combo.getPrice().multiply(BigDecimal.valueOf(e.getValue())));
            }
        }
        return total;
    }
}
