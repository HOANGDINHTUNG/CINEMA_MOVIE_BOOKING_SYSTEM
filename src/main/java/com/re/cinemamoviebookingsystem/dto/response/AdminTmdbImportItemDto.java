package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogSummaryDto;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminTmdbImportItemDto {
    MovieCatalogSummaryDto tmdb;
    AdminTmdbImportStateDto cinemaState;

    public boolean isPublishable() {
        return cinemaState != null && cinemaState.isPublishable();
    }
}
