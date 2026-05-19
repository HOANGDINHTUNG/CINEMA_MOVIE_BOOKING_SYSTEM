package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.RevenueReportDto;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public RevenueReportDto getReport(int year, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);
        BigDecimal total = bookingRepository.sumRevenue(fromDt, toDt);

        Map<Integer, BigDecimal> byMonth = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            byMonth.put(m, BigDecimal.ZERO);
        }
        for (Object[] row : bookingRepository.revenueByMonth(year)) {
            int month = ((Number) row[0]).intValue();
            BigDecimal revenue = row[1] instanceof BigDecimal b ? b : new BigDecimal(row[1].toString());
            byMonth.put(month, revenue);
        }

        List<RevenueReportDto.TopMovieDto> top = bookingRepository.topMoviesByRevenue().stream()
                .map(r -> RevenueReportDto.TopMovieDto.builder()
                        .title((String) r[0])
                        .revenue(r[1] instanceof BigDecimal b ? b : new BigDecimal(r[1].toString()))
                        .build())
                .collect(Collectors.toList());

        return RevenueReportDto.builder()
                .totalRevenue(total != null ? total : BigDecimal.ZERO)
                .revenueByMonth(byMonth)
                .topMovies(top)
                .build();
    }
}
