package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeMovieSectionsDto {
    private final List<CinemaMovieCardDto> nowShowing;
    private final List<CinemaMovieCardDto> comingSoon;
}
