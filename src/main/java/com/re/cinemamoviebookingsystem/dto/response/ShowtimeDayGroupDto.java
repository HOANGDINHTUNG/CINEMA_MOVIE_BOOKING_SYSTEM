package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ShowtimeDayGroupDto {
    private LocalDate date;
    private String dateLabel;
    private String monthLabel;
    private String dayNumber;
    private String weekdayLabel;
    private List<ShowtimeBrowseDto> slots;
}
