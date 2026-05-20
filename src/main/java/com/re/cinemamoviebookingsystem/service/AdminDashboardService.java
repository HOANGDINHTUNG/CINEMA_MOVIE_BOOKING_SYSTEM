package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.response.AdminBookingSummaryDto;
import com.re.cinemamoviebookingsystem.dto.response.AdminDashboardDto;
import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.repository.MovieRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final DateTimeFormatter CHART_DAY_LABEL =
            DateTimeFormatter.ofPattern("dd/MM");

    private final BookingRepository bookingRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CinemaProperties cinemaProperties;
    private final MovieDisplayService movieDisplayService;

    @Transactional(readOnly = true)
    public AdminDashboardDto loadDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        BigDecimal revenueToday = nullToZero(bookingRepository.sumRevenue(dayStart, dayEnd));
        BigDecimal revenueWeek = nullToZero(bookingRepository.sumRevenue(weekStart, dayEnd));
        BigDecimal revenueMonth = nullToZero(bookingRepository.sumRevenue(monthStart, dayEnd));

        LocalDateTime staleCutoff = LocalDateTime.now()
                .minusMinutes(cinemaProperties.getSeatLockMinutes());
        List<AdminBookingSummaryDto> stalePending = bookingRepository
                .findStalePendingWithDetails(BookingStatus.PENDING, staleCutoff)
                .stream()
                .limit(10)
                .map(this::toSummary)
                .collect(Collectors.toList());

        Map<LocalDate, BigDecimal> revenue7 = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            revenue7.put(today.minusDays(i), BigDecimal.ZERO);
        }
        for (Object[] row : bookingRepository.revenueByDay(weekStart)) {
            LocalDate d = row[0] instanceof java.sql.Date sd
                    ? sd.toLocalDate()
                    : LocalDate.parse(row[0].toString());
            BigDecimal rev = row[1] instanceof BigDecimal b ? b : new BigDecimal(row[1].toString());
            revenue7.put(d, rev);
        }

        List<String> chartLabels = new ArrayList<>(revenue7.size());
        List<BigDecimal> chartValues = new ArrayList<>(revenue7.size());
        for (Map.Entry<LocalDate, BigDecimal> entry : revenue7.entrySet()) {
            chartLabels.add(entry.getKey().format(CHART_DAY_LABEL));
            chartValues.add(entry.getValue());
        }

        return AdminDashboardDto.builder()
                .revenueToday(revenueToday)
                .revenueWeek(revenueWeek)
                .revenueMonth(revenueMonth)
                .pendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING))
                .paidBookings(bookingRepository.countByStatus(BookingStatus.PAID))
                .cancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .showtimesToday(showtimeRepository.countBetween(dayStart, dayEnd.plusSeconds(1)))
                .activeMovies(movieRepository.countByStatus(MovieStatus.ACTIVE))
                .revenueChartLabels(chartLabels)
                .revenueChartValues(chartValues)
                .stalePendingBookings(stalePending)
                .build();
    }

    private AdminBookingSummaryDto toSummary(Booking booking) {
        String customerName = booking.getUser().getProfile() != null
                ? booking.getUser().getProfile().getFullName()
                : booking.getUser().getUsername();
        String movieTitle = movieDisplayService.resolveTitle(
                booking.getShowtime().getMovie(), com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage.VI_VN);

        return AdminBookingSummaryDto.builder()
                .bookingId(booking.getBookingId())
                .customerName(customerName)
                .movieTitle(movieTitle)
                .showtimeStart(booking.getShowtime().getStartTime())
                .roomName(booking.getShowtime().getRoom().getRoomName())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .bookingDate(booking.getBookingDate())
                .build();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
