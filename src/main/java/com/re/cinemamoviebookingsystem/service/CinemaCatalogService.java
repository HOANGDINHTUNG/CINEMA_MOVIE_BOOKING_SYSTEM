package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.MovieDto;
import com.re.cinemamoviebookingsystem.dto.response.ScheduleMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.ShowtimeBrowseDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.CinemaMovieCardDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.HomeMovieSectionsDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogSummaryDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.TmdbGenreItemDto;
import com.re.cinemamoviebookingsystem.entity.Movie;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.MovieRepository;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import com.re.cinemamoviebookingsystem.util.MovieAgeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CinemaCatalogService {

    private final MovieRepository movieRepository;
    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final TmdbCatalogService tmdbCatalogService;

    @Transactional(readOnly = true)
    public HomeMovieSectionsDto listHomeMovieSections(AppLanguage lang) {
        return HomeMovieSectionsDto.builder()
                .nowShowing(listNowShowingAtCinema(lang))
                .comingSoon(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CinemaMovieCardDto> listNowShowingAtCinema(AppLanguage lang) {
        return listNowShowingPage(lang, 1, Integer.MAX_VALUE);
    }

    /**
     * Phim ACTIVE trong DB có ít nhất một suất tương lai (ACTIVE/SOLD_OUT).
     * Hiển thị: TMDB theo {@code tmdb_id}; suất gần nhất từ DB.
     */
    @Transactional(readOnly = true)
    public List<CinemaMovieCardDto> listNowShowingPage(AppLanguage lang, int page, int pageSize) {
        Map<Long, LocalDateTime> nextByTmdb = showtimeService.mapNextShowtimeByTmdbId();
        List<MovieDto> movies = movieService.listActiveWithUpcomingShowtimes(lang).stream()
                .filter(dto -> dto.getTmdbId() != null && nextByTmdb.containsKey(dto.getTmdbId()))
                .sorted(Comparator.comparing(dto -> nextByTmdb.get(dto.getTmdbId())))
                .collect(Collectors.toList());
        return sliceAndEnrich(movies, page, pageSize, lang, nextByTmdb, true);
    }

    @Transactional(readOnly = true)
    public int countNowShowingAtCinema() {
        return (int) movieService.listActiveWithUpcomingShowtimes(AppLanguage.VI_VN).stream()
                .filter(dto -> dto.getTmdbId() != null)
                .count();
    }

    /** Phim đã đăng rạp, chưa có suất (dùng cho demo seed). */
    @Transactional(readOnly = true)
    public List<CinemaMovieCardDto> listPublishedWithoutShowtimes(AppLanguage lang) {
        Set<Long> withShowtimes = showtimeService.mapNextShowtimeByTmdbId().keySet();
        List<MovieDto> movies = movieService.listActive(lang).stream()
                .filter(dto -> dto.getTmdbId() != null && !withShowtimes.contains(dto.getTmdbId()))
                .collect(Collectors.toList());
        return sliceAndEnrich(movies, 1, Integer.MAX_VALUE, lang, Map.of(), false);
    }

    /** Phim đăng rạp chưa có suất (demo seed). */
    @Transactional(readOnly = true)
    public int countPublishedWithoutShowtimes() {
        Set<Long> withShowtimes = showtimeService.mapNextShowtimeByTmdbId().keySet();
        return (int) movieRepository.findByStatusOrderByPublishedAtDesc(MovieStatus.ACTIVE).stream()
                .filter(m -> m.getTmdbId() != null && !withShowtimes.contains(m.getTmdbId()))
                .count();
    }

    /** @deprecated Dùng {@link #countPublishedWithoutShowtimes()} cho demo seed. */
    @Deprecated
    @Transactional(readOnly = true)
    public int countComingSoonAtCinema() {
        return countPublishedWithoutShowtimes();
    }

    private List<CinemaMovieCardDto> sliceAndEnrich(List<MovieDto> movies,
                                                      int page,
                                                      int pageSize,
                                                      AppLanguage lang,
                                                      Map<Long, LocalDateTime> nextByTmdb,
                                                      boolean hasShowtimes) {
        if (movies.isEmpty()) {
            return List.of();
        }
        int safePage = Math.max(1, page);
        int size = Math.max(1, pageSize);
        int from = (safePage - 1) * size;
        if (from >= movies.size()) {
            return List.of();
        }
        int to = Math.min(from + size, movies.size());
        return movies.subList(from, to).stream()
                .map(dto -> toCard(
                        dto,
                        enrichFromTmdb(dto, lang),
                        hasShowtimes,
                        hasShowtimes ? nextByTmdb.get(dto.getTmdbId()) : null))
                .collect(Collectors.toList());
    }

    public List<CinemaMovieCardDto> mapSummariesToCards(List<MovieCatalogSummaryDto> summaries,
                                                        Map<Integer, String> genreNameMap) {
        return mapSummariesToCards(summaries, genreNameMap, Map.of(), false);
    }

    public List<CinemaMovieCardDto> mapSummariesToCards(List<MovieCatalogSummaryDto> summaries,
                                                        Map<Integer, String> genreNameMap,
                                                        Map<Long, LocalDateTime> nextShowtimeByTmdbId,
                                                        boolean attachShowtimeInfo) {
        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }
        return summaries.stream()
                .map(s -> fromCatalogSummary(s, genreNameMap, nextShowtimeByTmdbId, attachShowtimeInfo))
                .collect(Collectors.toList());
    }

    public CinemaMovieCardDto fromCatalogSummary(MovieCatalogSummaryDto summary,
                                                   Map<Integer, String> genreNameMap) {
        return fromCatalogSummary(summary, genreNameMap, Map.of(), false);
    }

    public CinemaMovieCardDto fromCatalogSummary(MovieCatalogSummaryDto summary,
                                                   Map<Integer, String> genreNameMap,
                                                   Map<Long, LocalDateTime> nextShowtimeByTmdbId,
                                                   boolean attachShowtimeInfo) {
        String genresLabel = resolveGenresLabel(summary.getGenreIds(), genreNameMap);
        String overview = summary.getOverview();
        if (overview != null && overview.length() > 140) {
            overview = overview.substring(0, 137) + "...";
        }
        LocalDateTime next = attachShowtimeInfo && nextShowtimeByTmdbId != null
                ? nextShowtimeByTmdbId.get(summary.getTmdbId())
                : null;
        return CinemaMovieCardDto.builder()
                .tmdbId(summary.getTmdbId())
                .title(summary.getTitle())
                .overview(overview)
                .posterUrl(summary.getPosterUrl())
                .backdropUrl(summary.getBackdropUrl())
                .voteAverage(summary.getVoteAverage())
                .voteCount(summary.getVoteCount())
                .genresLabel(genresLabel)
                .releaseDateLabel(summary.getReleaseDate())
                .hasShowtimes(next != null)
                .nextShowtime(next)
                .build();
    }

    private static String resolveGenresLabel(List<Integer> genreIds, Map<Integer, String> genreNameMap) {
        if (genreIds == null || genreIds.isEmpty() || genreNameMap == null || genreNameMap.isEmpty()) {
            return null;
        }
        return genreIds.stream()
                .map(genreNameMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    @Transactional(readOnly = true)
    public List<CinemaMovieCardDto> listCinemaMoviesWithShowtimes(AppLanguage lang) {
        return listNowShowingAtCinema(lang);
    }

    @Transactional(readOnly = true)
    public CinemaMovieCardDto getCinemaMovie(Long movieId, AppLanguage lang) {
        MovieDto dto = movieService.findById(movieId, lang);
        if (dto.getStatus() != MovieStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Phim không khả dụng");
        }
        LocalDateTime next = showtimeService.browseUpcoming().stream()
                .filter(s -> Objects.equals(s.getMovieId(), movieId))
                .map(ShowtimeBrowseDto::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return toCard(dto, enrichFromTmdb(dto, lang), next != null, next);
    }

    @Transactional(readOnly = true)
    public List<ScheduleMovieCardDto> enrichScheduleMovies(List<ScheduleMovieCardDto> cards, AppLanguage lang) {
        if (cards == null || cards.isEmpty()) {
            return cards;
        }
        List<ScheduleMovieCardDto> enriched = new ArrayList<>();
        for (ScheduleMovieCardDto card : cards) {
            enriched.add(enrichScheduleCard(card, lang));
        }
        return enriched;
    }

    private ScheduleMovieCardDto enrichScheduleCard(ScheduleMovieCardDto card, AppLanguage lang) {
        if (card.getTmdbId() == null) {
            return card;
        }
        MovieCatalogDetailDto tmdb;
        try {
            tmdb = tmdbCatalogService.getDetail(lang, card.getTmdbId());
        } catch (Exception ex) {
            return card;
        }
        if (tmdb == null) {
            return card;
        }
        String title = card.getTitle();
        if (tmdb.getTitle() != null && !tmdb.getTitle().isBlank()) {
            title = tmdb.getTitle();
        }
        String poster = card.getPosterUrl();
        if (tmdb.getPosterUrl() != null) {
            poster = tmdb.getPosterUrl();
        }
        String overview = tmdb.getOverview();
        if (overview != null && overview.length() > 200) {
            overview = overview.substring(0, 197) + "...";
        }
        String genresLabel = card.getGenresLabel();
        if (tmdb.getGenres() != null && !tmdb.getGenres().isEmpty()) {
            genresLabel = tmdb.getGenres().stream()
                    .map(TmdbGenreItemDto::getName)
                    .collect(Collectors.joining(", "));
        }
        String ageLabel = card.getAgeLabel();
        if (ageLabel == null || ageLabel.isBlank()) {
            ageLabel = tmdb.getAgeLabel() != null ? tmdb.getAgeLabel()
                    : MovieAgeUtil.extractAgeLabel(title);
        }
        Integer duration = card.getDuration();
        if (tmdb.getRuntime() != null) {
            duration = tmdb.getRuntime();
        }
        java.time.LocalDate releaseDate = card.getReleaseDate();
        if (tmdb.getReleaseDate() != null && !tmdb.getReleaseDate().isBlank()) {
            try {
                releaseDate = java.time.LocalDate.parse(tmdb.getReleaseDate());
            } catch (Exception ignored) {
            }
        }
        String originLabel = resolveOriginLabel(tmdb);
        String originCountryCode = resolveOriginCountryCode(tmdb);
        String displayTitle = MovieAgeUtil.appendAgeSuffixToTitle(title, ageLabel);
        return ScheduleMovieCardDto.builder()
                .tmdbId(card.getTmdbId())
                .movieId(card.getMovieId())
                .title(displayTitle)
                .posterUrl(poster)
                .overview(overview)
                .voteAverage(tmdb.getVoteAverage())
                .genresLabel(genresLabel)
                .duration(duration)
                .releaseDate(releaseDate)
                .ageLabel(ageLabel)
                .ageNote(MovieAgeUtil.buildAgeNote(title, ageLabel))
                .format(card.getFormat())
                .originLabel(originLabel)
                .originCountryCode(originCountryCode != null ? originCountryCode : card.getOriginCountryCode())
                .slots(card.getSlots())
                .build();
    }

    private static String resolveOriginLabel(MovieCatalogDetailDto tmdb) {
        String code = resolveOriginCountryCode(tmdb);
        return code != null ? com.re.cinemamoviebookingsystem.util.CountryNames.label(code) : null;
    }

    private static String resolveOriginCountryCode(MovieCatalogDetailDto tmdb) {
        if (tmdb.getProductionCompanies() == null || tmdb.getProductionCompanies().isEmpty()) {
            return null;
        }
        for (var company : tmdb.getProductionCompanies()) {
            if (company.getOriginCountry() != null && !company.getOriginCountry().isBlank()) {
                return company.getOriginCountry().trim().toUpperCase(java.util.Locale.ROOT);
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public MovieCatalogDetailDto getCatalogDetailForMovie(Long movieId, AppLanguage lang) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phim không tồn tại"));
        if (movie.getTmdbId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Phim chưa liên kết TMDB");
        }
        return tmdbCatalogService.getDetail(lang, movie.getTmdbId());
    }

    private MovieCatalogDetailDto enrichFromTmdb(MovieDto dto, AppLanguage lang) {
        if (dto.getTmdbId() == null) {
            return null;
        }
        try {
            return tmdbCatalogService.getDetail(lang, dto.getTmdbId());
        } catch (Exception ex) {
            return null;
        }
    }

    private CinemaMovieCardDto toCard(MovieDto dto, MovieCatalogDetailDto tmdb, boolean hasShowtimes, LocalDateTime next) {
        String title = dto.getDisplayTitle();
        String overview = null;
        String poster = null;
        String backdrop = null;
        Integer duration = dto.getDuration();
        Double voteAverage = null;
        Integer voteCount = null;
        String genresLabel = null;
        String releaseDateLabel = null;
        String ageLabel = dto.getAgeLabel();

        if (tmdb != null) {
            if (tmdb.getTitle() != null && !tmdb.getTitle().isBlank()) {
                title = tmdb.getTitle();
            }
            overview = tmdb.getOverview();
            poster = tmdb.getPosterUrl();
            backdrop = tmdb.getBackdropUrl();
            voteAverage = tmdb.getVoteAverage();
            voteCount = tmdb.getVoteCount();
            if (tmdb.getRuntime() != null) {
                duration = tmdb.getRuntime();
            }
            if (tmdb.getGenres() != null && !tmdb.getGenres().isEmpty()) {
                genresLabel = tmdb.getGenres().stream()
                        .map(TmdbGenreItemDto::getName)
                        .collect(Collectors.joining(", "));
            }
            releaseDateLabel = tmdb.getReleaseDate();
            if (ageLabel == null || ageLabel.isBlank()) {
                ageLabel = tmdb.getAgeLabel();
            }
        }
        if (overview != null && overview.length() > 140) {
            overview = overview.substring(0, 137) + "...";
        }
        return CinemaMovieCardDto.builder()
                .movieId(dto.getMovieId())
                .tmdbId(dto.getTmdbId())
                .title(title)
                .overview(overview)
                .posterUrl(poster)
                .backdropUrl(backdrop)
                .duration(duration)
                .releaseDateLabel(releaseDateLabel)
                .genresLabel(genresLabel)
                .ageLabel(ageLabel)
                .voteAverage(voteAverage)
                .voteCount(voteCount)
                .ageLabel(ageLabel)
                .hasShowtimes(hasShowtimes)
                .nextShowtime(next)
                .build();
    }
}
