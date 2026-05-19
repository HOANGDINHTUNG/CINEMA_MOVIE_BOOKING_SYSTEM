package com.re.cinemamoviebookingsystem.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieImagesDto {
    private Long id;
    private List<TmdbImageFileDto> backdrops;
    private List<TmdbImageFileDto> posters;
    private List<TmdbImageFileDto> logos;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbImageFileDto {
        @JsonProperty("file_path")
        private String filePath;
        @JsonProperty("aspect_ratio")
        private Double aspectRatio;
        private Integer width;
        private Integer height;
    }
}
