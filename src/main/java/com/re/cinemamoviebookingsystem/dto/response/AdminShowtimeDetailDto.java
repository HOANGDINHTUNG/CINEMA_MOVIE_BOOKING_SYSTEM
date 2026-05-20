package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminShowtimeDetailDto {
    private Long showtimeId;
    private Long movieId;
    private Long tmdbId;
    private String movieTitle;
    private Integer roomId;
    private String roomName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private long availableSeats;
    private long lockedSeats;
    private long bookedSeats;
    private int totalSeats;
    private long paidBookings;
    private boolean canEdit;
    private boolean canCancel;
    private long showtimeSeatRows;
    private boolean seatsConfigured;
}
