package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminMoviePageResponse {
    private List<AdminMovieListItemDto> items;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasNext;
}
