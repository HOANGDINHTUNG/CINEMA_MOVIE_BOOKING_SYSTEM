package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeMoviesResponseDto {
    private List<CinemaMovieCardDto> movies;
    private boolean hasMore;
    private int page;
}
