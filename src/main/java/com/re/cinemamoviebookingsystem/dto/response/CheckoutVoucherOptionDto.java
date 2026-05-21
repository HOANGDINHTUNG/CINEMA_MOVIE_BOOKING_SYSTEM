package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CheckoutVoucherOptionDto {

    private Long userVoucherId;
    private String code;
    private String title;
    private String discountLabel;
    private String conditionLabel;
    /** Một dòng (fallback cho select cũ). */
    private String label;
    private BigDecimal estimatedDiscount;
    private boolean recommended;
}
