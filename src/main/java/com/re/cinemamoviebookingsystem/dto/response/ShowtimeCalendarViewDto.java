package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ShowtimeCalendarViewDto {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private LocalDate previousWeek;
    private LocalDate nextWeek;
    private Integer roomFilter;
    private List<RoomColumnDto> rooms;
    private List<LocalDate> days;
    /** roomId → (yyyy-MM-dd → events) */
    private Map<Integer, Map<String, List<ShowtimeCalendarEventDto>>> grid;

    @Getter
    @Builder
    public static class RoomColumnDto {
        private Integer roomId;
        private String roomName;
    }
}
