package com.re.cinemamoviebookingsystem.dto.response.catalog;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MovieCatalogPageDto {
    private int page;
    private int totalPages;
    private int totalResults;
    private List<MovieCatalogSummaryDto> results;
}
