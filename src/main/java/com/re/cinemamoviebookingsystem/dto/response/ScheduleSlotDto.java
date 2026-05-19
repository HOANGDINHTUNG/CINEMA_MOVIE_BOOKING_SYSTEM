package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleSlotDto {
    private Long showtimeId;
    private String timeLabel;
    private boolean soldOut;
}
