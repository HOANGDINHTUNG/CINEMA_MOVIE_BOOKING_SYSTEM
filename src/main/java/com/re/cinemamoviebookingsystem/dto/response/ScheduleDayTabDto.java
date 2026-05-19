package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ScheduleDayTabDto {
    private LocalDate date;
    private String label;
    private boolean selected;
}
