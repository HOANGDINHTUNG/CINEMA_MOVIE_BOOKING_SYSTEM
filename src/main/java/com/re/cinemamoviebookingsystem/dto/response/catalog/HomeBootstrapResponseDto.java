package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeBootstrapResponseDto {
    private HomeMoviesResponseDto nowShowing;
    private HomeMoviesResponseDto comingSoon;
    /** Xu hướng tuần TMDB — sidebar (~5 phim). */
    private List<CinemaMovieCardDto> trending;
    private List<HeroSlideDto> heroSlides;
    private String error;
}
