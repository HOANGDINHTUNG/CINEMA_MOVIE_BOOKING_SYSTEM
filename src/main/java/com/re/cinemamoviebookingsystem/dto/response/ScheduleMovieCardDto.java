package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ScheduleMovieCardDto {
    private Long tmdbId;
    private Long movieId;
    private String title;
    private String posterUrl;
    private String overview;
    private Double voteAverage;
    private String genresLabel;
    private Integer duration;
    private LocalDate releaseDate;
    private String ageLabel;
    private String ageNote;
    private String format;
    private List<ScheduleSlotDto> slots;
}
