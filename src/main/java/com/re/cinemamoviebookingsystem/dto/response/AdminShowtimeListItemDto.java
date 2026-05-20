package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminShowtimeListItemDto {
    private Long showtimeId;
    private Long movieId;
    private Long tmdbId;
    private String movieTitle;
    private String roomName;
    private Integer roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private long bookedSeats;
    private int totalSeats;
    private int fillPercent;
}
