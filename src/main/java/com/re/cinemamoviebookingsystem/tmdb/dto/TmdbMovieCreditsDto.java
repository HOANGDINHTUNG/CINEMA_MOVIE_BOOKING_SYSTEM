package com.re.cinemamoviebookingsystem.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieCreditsDto {
    private Long id;
    private List<TmdbCastMemberDto> cast;
    private List<TmdbCrewMemberDto> crew;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbCrewMemberDto {
        private Long id;
        private String name;
        private String job;
        private String department;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbCastMemberDto {
        private Long id;
        private String name;
        private String character;
        @JsonProperty("profile_path")
        private String profilePath;
        private Integer order;
    }
}
