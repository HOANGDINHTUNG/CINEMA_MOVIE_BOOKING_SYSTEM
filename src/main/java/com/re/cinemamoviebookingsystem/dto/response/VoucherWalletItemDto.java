package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.DiscountType;
import com.re.cinemamoviebookingsystem.enums.UserVoucherStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class VoucherWalletItemDto {

    private Long userVoucherId;
    private String code;
    private String title;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private boolean requireCombo;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private UserVoucherStatus status;
    private String discountLabel;
    private String conditionLabel;
    private boolean usableNow;
}
