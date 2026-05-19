package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HeroSlideDto {
    private String title;
    private String originalTitle;
    private String overview;
    private String backdropUrl;
    private String posterUrl;
    private Long tmdbId;
    private Long movieId;
    private Integer releaseYear;
    private Double voteAverage;
    private String languageLabel;
}
