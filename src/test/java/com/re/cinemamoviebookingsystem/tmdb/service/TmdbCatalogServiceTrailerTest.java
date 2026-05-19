package com.re.cinemamoviebookingsystem.tmdb.service;

import com.re.cinemamoviebookingsystem.tmdb.dto.TmdbMovieVideosDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TmdbCatalogServiceTrailerTest {

    @Test
    void resolveTrailerEmbedUrl_prefersOfficialTrailer() {
        var videos = new TmdbMovieVideosDto();
        var official = video("abc123", "Trailer", true);
        var other = video("xyz", "Teaser", false);
        videos.setResults(List.of(other, official));

        assertThat(TmdbCatalogService.resolveTrailerEmbedUrl(videos))
                .isEqualTo("https://www.youtube.com/embed/abc123");
        assertThat(TmdbCatalogService.resolveTrailer(videos))
                .isEqualTo("https://www.youtube.com/watch?v=abc123");
    }

    private static TmdbMovieVideosDto.TmdbVideoItemDto video(String key, String type, boolean official) {
        var item = new TmdbMovieVideosDto.TmdbVideoItemDto();
        item.setKey(key);
        item.setSite("YouTube");
        item.setType(type);
        item.setOfficial(official);
        return item;
    }
}
