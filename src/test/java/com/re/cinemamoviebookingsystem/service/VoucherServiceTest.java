package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Voucher;
import com.re.cinemamoviebookingsystem.enums.DiscountType;
import com.re.cinemamoviebookingsystem.enums.VoucherStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoucherServiceTest {

    private final VoucherService voucherService = new VoucherService(null);

    @Test
    void percentDiscountRespectsMaxCap() {
        Voucher v = Voucher.builder()
                .code("NCC10")
                .title("10%")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.valueOf(100_000))
                .maxDiscountAmount(BigDecimal.valueOf(30_000))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(VoucherStatus.ACTIVE)
                .build();

        BigDecimal discount = voucherService.calculateDiscount(v, BigDecimal.valueOf(500_000), false);
        assertEquals(0, BigDecimal.valueOf(30_000).compareTo(discount));
    }

    @Test
    void fixedDiscountCannotExceedSubtotal() {
        Voucher v = Voucher.builder()
                .code("NCC50K")
                .title("50K")
                .discountType(DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(50_000))
                .minOrderAmount(BigDecimal.valueOf(200_000))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(30))
                .status(VoucherStatus.ACTIVE)
                .build();

        BigDecimal discount = voucherService.calculateDiscount(v, BigDecimal.valueOf(120_000), false);
        assertEquals(0, BigDecimal.ZERO.compareTo(discount));
    }
}
