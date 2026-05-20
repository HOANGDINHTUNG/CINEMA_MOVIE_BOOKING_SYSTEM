package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AdminDashboardDto {
    private BigDecimal revenueToday;
    private BigDecimal revenueWeek;
    private BigDecimal revenueMonth;
    private long pendingBookings;
    private long paidBookings;
    private long cancelledBookings;
    private long showtimesToday;
    private long activeMovies;
    /** Nhãn ngày (dd/MM) cho Chart.js — chuỗi để Thymeleaf inline JS serialize an toàn. */
    private List<String> revenueChartLabels;
    private List<BigDecimal> revenueChartValues;
    private List<AdminBookingSummaryDto> stalePendingBookings;
}
