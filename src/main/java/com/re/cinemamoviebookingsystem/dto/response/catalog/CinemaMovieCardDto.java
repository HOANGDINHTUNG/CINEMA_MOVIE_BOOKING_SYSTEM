package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CinemaMovieCardDto {
    private Long movieId;
    private Long tmdbId;
    private String title;
    private String overview;
    private String posterUrl;
    private String backdropUrl;
    private Integer duration;
    private LocalDate releaseDate;
    private String trailerUrl;
    private boolean hasShowtimes;
    private LocalDateTime nextShowtime;
    private List<String> genreNames;
    private String genresLabel;
    private Double voteAverage;
    private Integer voteCount;
    private String releaseDateLabel;
    private String ageLabel;
}
