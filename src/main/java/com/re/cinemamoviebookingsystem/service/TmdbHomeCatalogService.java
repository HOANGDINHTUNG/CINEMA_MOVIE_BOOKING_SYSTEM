package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.catalog.CinemaMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.HomeBootstrapResponseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.HomeMoviesResponseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogPageDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogSummaryDto;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Trang chủ: phim đã đăng tại rạp (DB + TMDB enrich), phân trang «Tải thêm».
 */
@Service
@RequiredArgsConstructor
public class TmdbHomeCatalogService {

    public static final int PAGE_SIZE = 20;

    private final CinemaCatalogService cinemaCatalogService;
    private final HeroBannerService heroBannerService;
    private final TmdbCatalogService tmdbCatalogService;
    private final ShowtimeService showtimeService;

    public HomeBootstrapResponseDto loadHomeBootstrap(AppLanguage lang) {
        try {
            int nowTotal = cinemaCatalogService.countNowShowingAtCinema();
            int soonTotal = cinemaCatalogService.countComingSoonAtCinema();
            boolean noPublishedAtCinema = nowTotal == 0 && soonTotal == 0;
            String infoMessage = null;

            HomeMoviesResponseDto nowPage = buildNowShowingPage(lang, 1, nowTotal);
            HomeMoviesResponseDto soonPage = buildComingSoonPage(lang, 1, soonTotal);

            if (nowTotal == 0) {
                List<CinemaMovieCardDto> tmdbFallback = loadTmdbNowPlayingFallback(lang);
                if (!tmdbFallback.isEmpty()) {
                    nowPage = HomeMoviesResponseDto.builder()
                            .movies(tmdbFallback.stream().limit(PAGE_SIZE).toList())
                            .page(1)
                            .hasMore(tmdbFallback.size() > PAGE_SIZE)
                            .build();
                    if (noPublishedAtCinema) {
                        infoMessage = "home.no_published_use_catalog";
                    }
                }
            }

            List<CinemaMovieCardDto> heroSource = nowPage.getMovies() != null ? nowPage.getMovies() : List.of();
            List<MovieCatalogSummaryDto> heroSummaries = heroSource.stream()
                    .limit(5)
                    .map(this::toSummaryForHero)
                    .toList();

            return HomeBootstrapResponseDto.builder()
                    .nowShowing(nowPage)
                    .comingSoon(soonPage)
                    .heroSlides(heroBannerService.buildSlidesFromSummaries(heroSummaries, lang))
                    .infoMessage(infoMessage)
                    .noPublishedAtCinema(noPublishedAtCinema && nowTotal == 0)
                    .build();
        } catch (Exception ex) {
            return HomeBootstrapResponseDto.builder()
                    .nowShowing(emptyApi(1))
                    .comingSoon(emptyApi(1))
                    .heroSlides(List.of())
                    .error(ex.getMessage())
                    .build();
        }
    }

    public HomeMoviesResponseDto loadNowShowingApi(AppLanguage lang, int page) {
        int total = cinemaCatalogService.countNowShowingAtCinema();
        if (total == 0) {
            return loadTmdbNowPlayingPage(lang, page);
        }
        return buildNowShowingPage(lang, page, total);
    }

    public HomeMoviesResponseDto loadComingSoonApi(AppLanguage lang, int page) {
        int total = cinemaCatalogService.countComingSoonAtCinema();
        return buildComingSoonPage(lang, page, total);
    }

    private HomeMoviesResponseDto buildNowShowingPage(AppLanguage lang, int page, int total) {
        List<CinemaMovieCardDto> movies = cinemaCatalogService.listNowShowingPage(lang, page, PAGE_SIZE);
        return HomeMoviesResponseDto.builder()
                .movies(movies)
                .page(page)
                .hasMore((long) page * PAGE_SIZE < total)
                .build();
    }

    private HomeMoviesResponseDto buildComingSoonPage(AppLanguage lang, int page, int total) {
        List<CinemaMovieCardDto> movies = cinemaCatalogService.listComingSoonPage(lang, page, PAGE_SIZE);
        return HomeMoviesResponseDto.builder()
                .movies(movies)
                .page(page)
                .hasMore((long) page * PAGE_SIZE < total)
                .build();
    }

    private HomeMoviesResponseDto loadTmdbNowPlayingPage(AppLanguage lang, int page) {
        MovieCatalogPageDto batch = tmdbCatalogService.nowPlaying(lang, page);
        if (batch.getResults() == null || batch.getResults().isEmpty()) {
            return emptyApi(page);
        }
        Map<Integer, String> genreMap = safeGenreMap(lang);
        Map<Long, LocalDateTime> nextByTmdb = showtimeService.mapNextShowtimeByTmdbId();
        List<MovieCatalogSummaryDto> summaries = batch.getResults().stream()
                .filter(TmdbCatalogService::hasHomeListMetadata)
                .limit(PAGE_SIZE)
                .toList();
        List<CinemaMovieCardDto> cards = cinemaCatalogService.mapSummariesToCards(
                summaries, genreMap, nextByTmdb, true);
        boolean hasMore = batch.getTotalPages() > 0 && page < batch.getTotalPages();
        return HomeMoviesResponseDto.builder()
                .movies(cards)
                .page(page)
                .hasMore(hasMore)
                .build();
    }

    private List<CinemaMovieCardDto> loadTmdbNowPlayingFallback(AppLanguage lang) {
        HomeMoviesResponseDto page = loadTmdbNowPlayingPage(lang, 1);
        return page.getMovies() != null ? page.getMovies() : List.of();
    }

    private Map<Integer, String> safeGenreMap(AppLanguage lang) {
        try {
            return tmdbCatalogService.genreNameMap(lang);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private MovieCatalogSummaryDto toSummaryForHero(CinemaMovieCardDto card) {
        return MovieCatalogSummaryDto.builder()
                .tmdbId(card.getTmdbId())
                .title(card.getTitle())
                .overview(card.getOverview())
                .posterUrl(card.getPosterUrl())
                .backdropUrl(card.getBackdropUrl())
                .voteAverage(card.getVoteAverage())
                .releaseDate(card.getReleaseDateLabel())
                .build();
    }

    private static HomeMoviesResponseDto emptyApi(int page) {
        return HomeMoviesResponseDto.builder()
                .movies(List.of())
                .hasMore(false)
                .page(page)
                .build();
    }
}
