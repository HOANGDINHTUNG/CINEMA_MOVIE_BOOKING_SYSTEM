package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.AdminMovieScreeningPhase;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminMovieListItemDto {
    private Long movieId;
    private Long tmdbId;
    private Integer duration;
    private String ageLabel;
    private MovieStatus status;
    private LocalDateTime publishedAt;
    private BigDecimal defaultBasePrice;
    private String displayTitle;
    private String posterUrl;
    private AdminMovieScreeningPhase phase;
    private long totalShowtimes;
    private long upcomingShowtimes;
    /** Đơn HELD / PENDING / PAID — có khách đặt thì không được ẩn phim. */
    private long audienceBookings;
    private boolean canDeactivate;
    private LocalDateTime nextShowtimeAt;
    private LocalDateTime lastShowtimeAt;
}
