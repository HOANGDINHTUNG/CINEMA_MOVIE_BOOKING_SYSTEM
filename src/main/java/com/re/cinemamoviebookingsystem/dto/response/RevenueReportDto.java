package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class RevenueReportDto {
    private BigDecimal totalRevenue;
    private Map<Integer, BigDecimal> revenueByMonth;
    private List<TopMovieDto> topMovies;

    @Getter
    @Builder
    public static class TopMovieDto {
        private String title;
        private BigDecimal revenue;
    }
}
