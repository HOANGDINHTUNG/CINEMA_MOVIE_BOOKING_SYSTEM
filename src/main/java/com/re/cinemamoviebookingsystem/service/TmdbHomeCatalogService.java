package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Trang chủ: «đang chiếu» = {@code movies} + suất (DB), poster/tên (TMDB theo {@code tmdb_id});
 * «sắp chiếu» = TMDB upcoming (tối đa {@link CinemaProperties#getHomeComingSoonMax()}).
 */
@Service
@RequiredArgsConstructor
public class TmdbHomeCatalogService {

    public static final int PAGE_SIZE = 20;
    public static final int TRENDING_SIDEBAR_SIZE = 5;
    public static final String TRENDING_WINDOW = "week";

    private final CinemaProperties cinemaProperties;
    private final CinemaCatalogService cinemaCatalogService;
    private final HeroBannerService heroBannerService;
    private final TmdbCatalogService tmdbCatalogService;
    private final ShowtimeService showtimeService;

    public HomeBootstrapResponseDto loadHomeBootstrap(AppLanguage lang) {
        try {
            HomeMoviesResponseDto nowPage = loadNowShowingApi(lang, 1);
            HomeMoviesResponseDto soonPage = loadComingSoonApi(lang, 1);
            List<CinemaMovieCardDto> trending = loadTrendingSidebar(lang, TRENDING_WINDOW);

            List<CinemaMovieCardDto> heroSource = nowPage.getMovies() != null ? nowPage.getMovies() : List.of();
            List<MovieCatalogSummaryDto> heroSummaries = heroSource.stream()
                    .limit(5)
                    .map(this::toSummaryForHero)
                    .toList();

            return HomeBootstrapResponseDto.builder()
                    .nowShowing(nowPage)
                    .comingSoon(soonPage)
                    .trending(trending)
                    .heroSlides(heroBannerService.buildSlidesFromSummaries(heroSummaries, lang))
                    .build();
        } catch (Exception ex) {
            return HomeBootstrapResponseDto.builder()
                    .nowShowing(emptyApi(1))
                    .comingSoon(emptyApi(1))
                    .trending(List.of())
                    .heroSlides(List.of())
                    .error(ex.getMessage())
                    .build();
        }
    }

    public HomeMoviesResponseDto loadNowShowingApi(AppLanguage lang, int page) {
        return loadCinemaNowShowingPage(lang, page);
    }

    public HomeMoviesResponseDto loadComingSoonApi(AppLanguage lang, int page) {
        return loadTmdbComingSoonPage(lang, page);
    }

    private HomeMoviesResponseDto loadCinemaNowShowingPage(AppLanguage lang, int page) {
        int safePage = Math.max(1, page);
        List<CinemaMovieCardDto> movies = cinemaCatalogService.listNowShowingPage(lang, safePage, PAGE_SIZE);
        int total = cinemaCatalogService.countNowShowingAtCinema();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / PAGE_SIZE);
        boolean hasMore = totalPages > 0 && safePage < totalPages;
        return HomeMoviesResponseDto.builder()
                .movies(movies)
                .page(safePage)
                .hasMore(hasMore)
                .build();
    }

    /**
     * TMDB upcoming (tối đa {@link CinemaProperties#getHomeComingSoonMax()}, mặc định 100), phân trang 20/trang.
     */
    private HomeMoviesResponseDto loadTmdbComingSoonPage(AppLanguage lang, int page) {
        int safePage = Math.max(1, page);
        int maxItems = Math.max(20, cinemaProperties.getHomeComingSoonMax());
        List<MovieCatalogSummaryDto> all = tmdbCatalogService.upcomingUpTo(lang, maxItems);
        if (all.isEmpty()) {
            return emptyApi(safePage);
        }
        int from = (safePage - 1) * PAGE_SIZE;
        if (from >= all.size()) {
            return emptyApi(safePage);
        }
        int to = Math.min(from + PAGE_SIZE, all.size());
        List<MovieCatalogSummaryDto> summaries = all.subList(from, to);
        Map<Integer, String> genreMap = safeGenreMap(lang);
        Map<Long, LocalDateTime> nextByTmdb = showtimeService.mapNextShowtimeByTmdbId();
        List<CinemaMovieCardDto> cards = cinemaCatalogService.mapSummariesToCards(
                summaries, genreMap, nextByTmdb, true);
        boolean hasMore = to < all.size();
        return HomeMoviesResponseDto.builder()
                .movies(cards)
                .page(safePage)
                .hasMore(hasMore)
                .build();
    }

    public List<CinemaMovieCardDto> loadTrendingSidebar(AppLanguage lang, String window) {
        Map<Long, LocalDateTime> nextByTmdb = showtimeService.mapNextShowtimeByTmdbId();
        Set<Long> cinemaTmdbIds = nextByTmdb.keySet();
        if (cinemaTmdbIds.isEmpty()) {
            return List.of();
        }

        String trendingWindow = normalizeTrendingWindow(window);
        List<CinemaMovieCardDto> result = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();

        try {
            MovieCatalogPageDto batch = tmdbCatalogService.trending(lang, 1, trendingWindow);
            if (batch.getResults() != null) {
                Map<Integer, String> genreMap = safeGenreMap(lang);
                List<MovieCatalogSummaryDto> summaries = batch.getResults().stream()
                        .filter(TmdbCatalogService::hasHomeListMetadata)
                        .filter(s -> cinemaTmdbIds.contains(s.getTmdbId()))
                        .limit(TRENDING_SIDEBAR_SIZE)
                        .toList();
                for (CinemaMovieCardDto card : cinemaCatalogService.mapSummariesToCards(
                        summaries, genreMap, nextByTmdb, true)) {
                    if (card.getTmdbId() != null && seen.add(card.getTmdbId())) {
                        result.add(card);
                    }
                }
            }
        } catch (Exception ignored) {
            // fallback below
        }

        if (result.size() < TRENDING_SIDEBAR_SIZE) {
            for (CinemaMovieCardDto card : cinemaCatalogService.listNowShowingPage(lang, 1, TRENDING_SIDEBAR_SIZE)) {
                if (card.getTmdbId() != null && seen.add(card.getTmdbId())) {
                    result.add(card);
                }
                if (result.size() >= TRENDING_SIDEBAR_SIZE) {
                    break;
                }
            }
        }
        return result.size() > TRENDING_SIDEBAR_SIZE
                ? result.subList(0, TRENDING_SIDEBAR_SIZE)
                : result;
    }

    private static String normalizeTrendingWindow(String window) {
        return window != null && window.equalsIgnoreCase("day") ? "day" : "week";
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
