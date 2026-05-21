package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MoviePicklistItemDto {
    private Long movieId;
    private Long tmdbId;
    private Integer duration;
    private BigDecimal defaultBasePrice;
    private String displayTitle;
}
