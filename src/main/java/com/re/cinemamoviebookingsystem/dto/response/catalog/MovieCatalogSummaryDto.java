package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MovieCatalogSummaryDto {
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private String overview;
    private String posterUrl;
    private String backdropUrl;
    private String releaseDate;
    private Double voteAverage;
    private Integer voteCount;
    private Double popularity;
    private List<Integer> genreIds;
}
