package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeBootstrapResponseDto {
    private HomeMoviesResponseDto nowShowing;
    private HomeMoviesResponseDto comingSoon;
    private List<HeroSlideDto> heroSlides;
    private String error;
    /** Message key gợi ý (chưa đăng phim tại rạp, v.v.). */
    private String infoMessage;
    private boolean noPublishedAtCinema;
}
