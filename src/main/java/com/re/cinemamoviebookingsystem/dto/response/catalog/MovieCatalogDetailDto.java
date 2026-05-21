package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MovieCatalogDetailDto {
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private String overview;
    private String posterUrl;
    private String posterPath;
    private String backdropUrl;
    private String releaseDate;
    private Double voteAverage;
    private Integer voteCount;
    private Integer runtime;
    private String tagline;
    private String status;
    private String imdbId;
    private String trailerUrl;
    private String trailerEmbedUrl;
    private String trailerYoutubeKey;
    private List<TmdbGenreItemDto> genres;
    private List<CastMemberDto> cast;
    private int castTotalCount;
    private List<MovieVideoClipDto> videoClips;
    private int videoClipTotalCount;
    private List<String> backdropGalleryUrls;
    private List<String> posterGalleryUrls;
    private List<String> logoGalleryUrls;
    private List<MovieCatalogSummaryDto> similarMovies;
    private List<MovieCatalogSummaryDto> recommendedMovies;
    private String director;
    private List<String> writers;
    private List<ProductionCompanyDto> productionCompanies;
    private String ageLabel;
}
