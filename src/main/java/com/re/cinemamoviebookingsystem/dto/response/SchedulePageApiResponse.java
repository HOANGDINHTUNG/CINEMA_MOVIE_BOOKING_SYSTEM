package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class SchedulePageApiResponse {
    List<ScheduleDayTabDto> days;
    LocalDate selectedDate;
    List<ScheduleMovieCardDto> movies;
    int resultCount;
    boolean filtering;
    String selectedGenreName;
    List<TmdbGenreItemDto> genres;
}
