package com.re.cinemamoviebookingsystem.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MovieCinemaUpdateRequest {

    @Min(1)
    private Integer duration;

    private String ageLabel;
    private BigDecimal defaultBasePrice;
    private String adminNote;
}
