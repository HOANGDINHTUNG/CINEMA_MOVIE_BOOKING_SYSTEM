package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.catalog.HeroSlideDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogSummaryDto;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HeroBannerService {

    private static final int MAX_SLIDES = 5;

    public List<HeroSlideDto> buildSlidesFromSummaries(List<MovieCatalogSummaryDto> summaries, AppLanguage language) {
        String langLabel = language == AppLanguage.EN_US ? "EN" : "VI";
        List<HeroSlideDto> slides = new ArrayList<>();
        if (summaries == null) {
            return slides;
        }
        for (MovieCatalogSummaryDto item : summaries) {
            HeroSlideDto slide = fromCatalogItem(item, langLabel);
            if (slide != null) {
                slides.add(slide);
            }
            if (slides.size() >= MAX_SLIDES) {
                break;
            }
        }
        return slides;
    }

    private static HeroSlideDto fromCatalogItem(MovieCatalogSummaryDto item, String langLabel) {
        String image = item.getBackdropUrl() != null ? item.getBackdropUrl() : item.getPosterUrl();
        if (image == null) {
            return null;
        }
        return HeroSlideDto.builder()
                .title(item.getTitle())
                .originalTitle(item.getOriginalTitle())
                .overview(item.getOverview())
                .backdropUrl(item.getBackdropUrl())
                .posterUrl(item.getPosterUrl())
                .tmdbId(item.getTmdbId())
                .releaseYear(releaseYear(item.getReleaseDate()))
                .voteAverage(item.getVoteAverage())
                .languageLabel(langLabel)
                .build();
    }

    private static Integer releaseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
