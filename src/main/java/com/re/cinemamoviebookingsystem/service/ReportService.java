package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.RevenueReportDto;
import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingRepository bookingRepository;
    private final MovieDisplayService movieDisplayService;

    @Transactional(readOnly = true)
    public RevenueReportDto getReport(int year, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);
        BigDecimal total = nullToZero(bookingRepository.sumRevenue(fromDt, toDt));

        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);
        BigDecimal previous = nullToZero(bookingRepository.sumRevenue(
                prevFrom.atStartOfDay(), prevTo.atTime(LocalTime.MAX)));
        Integer changePercent = percentChange(previous, total);

        Map<Integer, BigDecimal> byMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            byMonth.put(m, BigDecimal.ZERO);
        }
        for (Object[] row : bookingRepository.revenueByMonth(year)) {
            int month = ((Number) row[0]).intValue();
            BigDecimal revenue = row[1] instanceof BigDecimal b ? b : new BigDecimal(row[1].toString());
            byMonth.put(month, revenue);
        }

        List<RevenueReportDto.TopMovieDto> topMovies = bookingRepository.topMoviesByRevenueBetween(fromDt, toDt)
                .stream()
                .map(r -> {
                    Long tmdbId = r[1] instanceof Number n ? n.longValue() : Long.parseLong(r[1].toString());
                    BigDecimal revenue = r[2] instanceof BigDecimal b ? b : new BigDecimal(r[2].toString());
                    return RevenueReportDto.TopMovieDto.builder()
                            .title(movieDisplayService.resolveTitle(tmdbId, AppLanguage.VI_VN))
                            .revenue(revenue)
                            .build();
                })
                .collect(Collectors.toList());

        List<RevenueReportDto.TopRoomDto> topRooms = bookingRepository.topRoomsByRevenue(fromDt, toDt).stream()
                .map(r -> RevenueReportDto.TopRoomDto.builder()
                        .roomName((String) r[0])
                        .revenue(r[1] instanceof BigDecimal b ? b : new BigDecimal(r[1].toString()))
                        .bookingCount(r[2] instanceof Number n ? n.longValue() : Long.parseLong(r[2].toString()))
                        .build())
                .collect(Collectors.toList());

        List<String> monthLabels = new ArrayList<>();
        List<Double> monthValues = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : byMonth.entrySet()) {
            monthLabels.add("T" + entry.getKey());
            monthValues.add(entry.getValue().doubleValue());
        }

        return RevenueReportDto.builder()
                .totalRevenue(total)
                .previousPeriodRevenue(previous)
                .revenueChangePercent(changePercent)
                .revenueByMonth(byMonth)
                .revenueMonthLabels(monthLabels)
                .revenueMonthValues(monthValues)
                .topMovies(topMovies)
                .topRooms(topRooms)
                .paidBookings(bookingRepository.countByStatusAndBookingDateBetween(
                        BookingStatus.PAID, fromDt, toDt))
                .pendingBookings(bookingRepository.countByStatusAndBookingDateBetween(
                        BookingStatus.PENDING, fromDt, toDt))
                .cancelledBookings(bookingRepository.countByStatusAndBookingDateBetween(
                        BookingStatus.CANCELLED, fromDt, toDt))
                .build();
    }

    @Transactional(readOnly = true)
    public List<Booking> listBookingsForExport(LocalDate from, LocalDate to, BookingStatus status) {
        return bookingRepository.findForExport(
                from.atStartOfDay(), to.atTime(LocalTime.MAX), status);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Integer percentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
