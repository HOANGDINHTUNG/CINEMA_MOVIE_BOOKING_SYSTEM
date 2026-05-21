package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class VoucherPreviewDto {

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal grandTotal;
    private String label;
}
