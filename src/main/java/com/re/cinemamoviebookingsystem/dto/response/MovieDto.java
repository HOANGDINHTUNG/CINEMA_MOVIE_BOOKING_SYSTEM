package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class MovieDto {
    private Long movieId;
    private Long tmdbId;
    private Integer duration;
    private String ageLabel;
    private MovieStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime unpublishedAt;
    private BigDecimal defaultBasePrice;
    private String adminNote;
    /** Tiêu đề từ TMDB (chỉ hiển thị, không lưu DB). */
    private String displayTitle;
}
