package com.re.cinemamoviebookingsystem.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbGenreListResponseDto {
    private List<TmdbGenreDto> genres;
}
