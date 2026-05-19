package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeSectionResultDto {
    private List<CinemaMovieCardDto> cards;
    private List<MovieCatalogSummaryDto> summaries;
    private boolean hasMore;
    private int page;

    public static HomeSectionResultDto empty() {
        return HomeSectionResultDto.builder()
                .cards(List.of())
                .summaries(List.of())
                .hasMore(false)
                .page(1)
                .build();
    }
}
