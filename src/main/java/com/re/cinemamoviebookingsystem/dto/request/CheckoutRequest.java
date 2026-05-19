package com.re.cinemamoviebookingsystem.dto.request;

import com.re.cinemamoviebookingsystem.enums.PaymentMode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CheckoutRequest {

    @NotNull
    private Long showtimeId;

    @NotEmpty
    private List<Long> seatIds;

    @NotNull
    private PaymentMode paymentMode;

    private Map<Integer, Integer> comboQuantities;
}
