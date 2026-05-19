package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TmdbGenreItemDto {
    private Integer id;
    private String name;
}
