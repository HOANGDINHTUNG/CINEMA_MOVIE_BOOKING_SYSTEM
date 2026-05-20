package com.re.cinemamoviebookingsystem.repository.projection;

import java.time.LocalDateTime;

public interface MovieShowtimeStatsRow {
    Long getMovieId();

    Long getTotalCount();

    Long getUpcomingCount();

    LocalDateTime getNextStartTime();

    LocalDateTime getLastStartTime();
}
