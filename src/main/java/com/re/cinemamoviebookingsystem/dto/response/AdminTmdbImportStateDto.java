package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.AdminMovieScreeningPhase;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminTmdbImportStateDto {
    Long movieId;
    AdminMovieScreeningPhase phase;
    boolean publishable;

    public boolean isInCinema() {
        return movieId != null;
    }
}
