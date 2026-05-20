package com.re.cinemamoviebookingsystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ShowtimeBulkCreateRequest {

    private Long movieId;

    private Long tmdbId;

    @NotNull
    private Integer roomId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @DecimalMin("0")
    private BigDecimal basePrice;

    /** Khung giờ: 10:00,14:00,18:00 */
    private String timeSlots;
}
