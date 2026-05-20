package com.re.cinemamoviebookingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ShowtimeUpdateRequest {

    @NotNull
    private Integer roomId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    @DecimalMin("0")
    private BigDecimal basePrice;
}
