package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminBookingSummaryDto {
    private Long bookingId;
    private String customerName;
    private String movieTitle;
    private LocalDateTime showtimeStart;
    private String roomName;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime bookingDate;
}
