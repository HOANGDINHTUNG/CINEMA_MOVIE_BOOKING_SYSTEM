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
    private BigDecimal previousPeriodRevenue;
    private Integer revenueChangePercent;
    private Map<Integer, BigDecimal> revenueByMonth;
    private List<String> revenueMonthLabels;
    private List<Double> revenueMonthValues;
    private List<TopMovieDto> topMovies;
    private List<TopRoomDto> topRooms;
    private long paidBookings;
    private long pendingBookings;
    private long cancelledBookings;

    @Getter
    @Builder
    public static class TopMovieDto {
        private String title;
        private BigDecimal revenue;
    }

    @Getter
    @Builder
    public static class TopRoomDto {
        private String roomName;
        private BigDecimal revenue;
        private long bookingCount;
    }
}
