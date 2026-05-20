package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BookingHistoryDto {
    private Long bookingId;
    private LocalDateTime bookingDate;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private String movieTitle;
    private LocalDateTime showtimeStart;
    private String roomName;
    private List<String> seatLabels;
    private List<String> ticketCodes;
    private boolean cancellable;
    private Long showtimeId;
    private LocalDateTime lockExpiresAt;
    /** HELD: còn ghế đang giữ — cho phép tiếp tục đặt vé. */
    private boolean heldActive;
}
