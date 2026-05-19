package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ScheduleViewDto {
    private List<ScheduleDayTabDto> days;
    private LocalDate selectedDate;
    private List<ScheduleMovieCardDto> movies;
    private boolean filtering;
}
