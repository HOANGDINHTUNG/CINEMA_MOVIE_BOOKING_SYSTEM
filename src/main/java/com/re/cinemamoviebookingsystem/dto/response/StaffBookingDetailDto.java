package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class StaffBookingDetailDto {
    private Long bookingId;
    private String customerName;
    private String customerPhone;
    private String movieTitle;
    private LocalDateTime showtimeStart;
    private String roomName;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private List<String> seatLabels;
    private List<String> ticketCodes;
}
