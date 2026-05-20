package com.re.cinemamoviebookingsystem.service.admin;

import java.time.LocalDateTime;

public record MovieShowtimeStats(
        Long movieId,
        long totalCount,
        long upcomingCount,
        LocalDateTime nextStartTime,
        LocalDateTime lastStartTime
) {
}
