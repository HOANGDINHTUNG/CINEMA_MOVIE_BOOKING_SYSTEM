package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ShowtimeBrowseDto {
    private Long showtimeId;
    private Long movieId;
    private Long tmdbId;
    private String ageLabel;
    private String movieTitle;
    private String roomName;
    private LocalDateTime startTime;
    private BigDecimal basePrice;
    private ShowtimeStatus status;
    private boolean soldOut;
    private long availableSeats;
    private int totalSeats;
}
