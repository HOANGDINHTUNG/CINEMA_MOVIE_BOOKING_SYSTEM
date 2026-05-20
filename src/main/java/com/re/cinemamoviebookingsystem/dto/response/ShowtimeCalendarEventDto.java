package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ShowtimeCalendarEventDto {
    private Long showtimeId;
    private Integer roomId;
    private String roomName;
    private String movieTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ShowtimeStatus status;
    private int fillPercent;
}
