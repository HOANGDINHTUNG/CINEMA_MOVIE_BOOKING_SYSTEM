package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ShowtimeConflictCheckDto {
    private boolean conflict;
    private String message;
    private LocalDateTime proposedStart;
    private LocalDateTime proposedEnd;
    private LocalDateTime suggestedStart;
    private int cleaningBufferMinutes;
    private List<ConflictingShowtimeDto> conflicts;

    @Getter
    @Builder
    public static class ConflictingShowtimeDto {
        private Long showtimeId;
        private String movieTitle;
        private String roomName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
