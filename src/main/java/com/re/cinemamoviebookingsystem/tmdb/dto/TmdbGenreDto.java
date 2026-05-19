package com.re.cinemamoviebookingsystem.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbGenreDto {
    private Integer id;
    private String name;
}
