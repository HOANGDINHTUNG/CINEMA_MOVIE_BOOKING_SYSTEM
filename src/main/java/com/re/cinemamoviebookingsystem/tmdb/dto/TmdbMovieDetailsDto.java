package com.re.cinemamoviebookingsystem.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieDetailsDto {
    private Long id;
    private String title;
    @JsonProperty("original_title")
    private String originalTitle;
    private String overview;
    @JsonProperty("poster_path")
    private String posterPath;
    @JsonProperty("backdrop_path")
    private String backdropPath;
    @JsonProperty("release_date")
    private String releaseDate;
    @JsonProperty("vote_average")
    private Double voteAverage;
    @JsonProperty("vote_count")
    private Integer voteCount;
    private Integer runtime;
    private String tagline;
    private String status;
    @JsonProperty("imdb_id")
    private String imdbId;
    private List<TmdbGenreDto> genres;
    private TmdbMovieVideosDto videos;
    private TmdbMovieCreditsDto credits;
    @JsonProperty("production_companies")
    private List<TmdbProductionCompanyDto> productionCompanies;
    private TmdbMovieImagesDto images;
    private TmdbPagedResponseDto<TmdbMovieSummaryDto> similar;
    private TmdbPagedResponseDto<TmdbMovieSummaryDto> recommendations;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbProductionCompanyDto {
        private Long id;
        private String name;
        @JsonProperty("logo_path")
        private String logoPath;
        @JsonProperty("origin_country")
        private String originCountry;
    }
}
