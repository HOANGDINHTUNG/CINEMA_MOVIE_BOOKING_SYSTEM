package com.re.cinemamoviebookingsystem.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieVideosDto {
    private Long id;
    private List<TmdbVideoItemDto> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbVideoItemDto {
        private String name;
        private String key;
        private String site;
        private String type;
        private Boolean official;
        @JsonProperty("published_at")
        private String publishedAt;
        @JsonProperty("iso_639_1")
        private String iso6391;
    }
}
