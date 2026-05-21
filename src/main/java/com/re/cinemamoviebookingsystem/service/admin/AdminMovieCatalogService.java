package com.re.cinemamoviebookingsystem.service.admin;

import com.re.cinemamoviebookingsystem.dto.response.AdminMovieListItemDto;
import com.re.cinemamoviebookingsystem.dto.response.AdminMoviePageResponse;
import com.re.cinemamoviebookingsystem.dto.response.AdminMoviePhaseCountsDto;
import com.re.cinemamoviebookingsystem.dto.response.AdminTmdbImportStateDto;
import com.re.cinemamoviebookingsystem.entity.Movie;
import com.re.cinemamoviebookingsystem.enums.AdminMovieScreeningPhase;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.repository.MovieRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeRepository;
import com.re.cinemamoviebookingsystem.repository.projection.MovieAudienceBookingRow;
import com.re.cinemamoviebookingsystem.repository.projection.MovieShowtimeStatsRow;
import com.re.cinemamoviebookingsystem.service.MovieDisplayService;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
import com.re.cinemamoviebookingsystem.tmdb.util.TmdbImageUrlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminMovieCatalogService {

    /** Số phim mỗi lần tải (cuộn vô hạn) — mục đang đợi lịch chiếu. */
    public static final int WAITING_SCHEDULE_PAGE_SIZE = 20;

    private static final int STATS_BATCH_SIZE = 40;

    private static final List<ShowtimeStatus> UPCOMING_STATUSES =
            List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);

    private static final List<BookingStatus> AUDIENCE_BOOKING_STATUSES =
            List.of(BookingStatus.HELD, BookingStatus.PENDING, BookingStatus.PAID);

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final MovieDisplayService movieDisplayService;
    private final TmdbImageUrlBuilder tmdbImageUrlBuilder;
    private final TmdbCatalogService tmdbCatalogService;

    @Transactional(readOnly = true)
    public Map<Long, AdminTmdbImportStateDto> buildTmdbImportStateMap() {
        List<Movie> movies = movieRepository.findAll(Sort.by(Sort.Direction.DESC, "publishedAt"));
        Map<Long, MovieShowtimeStats> statsMap = loadStats(movies.stream().map(Movie::getMovieId).toList());
        Map<Long, AdminTmdbImportStateDto> map = new HashMap<>();
        for (Movie movie : movies) {
            if (movie.getTmdbId() == null) {
                continue;
            }
            MovieShowtimeStats stats = statsMap.get(movie.getMovieId());
            AdminMovieScreeningPhase phase = resolvePhase(movie.getStatus(), stats);
            map.put(movie.getTmdbId(), AdminTmdbImportStateDto.builder()
                    .movieId(movie.getMovieId())
                    .phase(phase)
                    .publishable(false)
                    .build());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public AdminTmdbImportStateDto stateForTmdbId(long tmdbId, Map<Long, AdminTmdbImportStateDto> cache) {
        AdminTmdbImportStateDto existing = cache.get(tmdbId);
        if (existing != null) {
            return existing;
        }
        return AdminTmdbImportStateDto.builder()
                .movieId(null)
                .phase(null)
                .publishable(true)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminMoviePhaseCountsDto countByPhase() {
        return countByPhase(null, AppLanguage.VI_VN);
    }

    @Transactional(readOnly = true)
    public AdminMoviePhaseCountsDto countByPhase(String keyword, AppLanguage lang) {
        Long tmdbFilter = parseNumericQuery(keyword);
        String likeKeyword = toLikeKeyword(keyword, tmdbFilter);
        LocalDateTime now = LocalDateTime.now();
        long waiting = movieRepository.countActiveWaitingSchedule(tmdbFilter, likeKeyword);
        long hasSchedule = movieRepository.countActiveHasUpcomingSchedule(
                now, UPCOMING_STATUSES, tmdbFilter, likeKeyword);
        long ended = movieRepository.countActiveEndedAtCinema(
                now, UPCOMING_STATUSES, tmdbFilter, likeKeyword);
        long inactive = movieRepository.countInactiveForAdmin(tmdbFilter, likeKeyword);
        return AdminMoviePhaseCountsDto.builder()
                .hasSchedule(hasSchedule)
                .waitingSchedule(waiting)
                .ended(ended)
                .inactive(inactive)
                .total(waiting + hasSchedule + ended + inactive)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listSection(AdminMovieScreeningPhase phase,
                                                   String keyword,
                                                   Pageable pageable,
                                                   AppLanguage lang) {
        return switch (phase) {
            case HAS_SCHEDULE -> listHasSchedule(keyword, pageable, lang);
            case WAITING_SCHEDULE -> listWaitingSchedule(keyword, pageable, lang);
            case ENDED -> listEndedAtCinema(keyword, pageable, lang);
            default -> Page.empty(pageable);
        };
    }

    /** Đã có lịch chiếu tại rạp (có suất sắp tới). */
    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listHasSchedule(String keyword, Pageable pageable, AppLanguage lang) {
        Long tmdbFilter = parseNumericQuery(keyword);
        String likeKeyword = toLikeKeyword(keyword, tmdbFilter);
        Page<Movie> page = movieRepository.findActiveHasUpcomingSchedule(
                LocalDateTime.now(), UPCOMING_STATUSES, tmdbFilter, likeKeyword, pageable);
        return mapMoviePage(page, AdminMovieScreeningPhase.HAS_SCHEDULE, lang);
    }

    /** Đang đợi xếp lịch — đã đăng rạp, chưa có suất. */
    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listWaitingSchedule(String keyword, Pageable pageable, AppLanguage lang) {
        Long tmdbFilter = parseNumericQuery(keyword);
        String likeKeyword = toLikeKeyword(keyword, tmdbFilter);
        Page<Movie> page = movieRepository.findActiveWaitingSchedule(tmdbFilter, likeKeyword, pageable);
        return mapMoviePage(page, AdminMovieScreeningPhase.WAITING_SCHEDULE, lang);
    }

    /** Đã ẩn khỏi rạp — chỉ status INACTIVE. */
    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listInactive(String keyword, Pageable pageable, AppLanguage lang) {
        Long tmdbFilter = parseNumericQuery(keyword);
        String likeKeyword = toLikeKeyword(keyword, tmdbFilter);
        Page<Movie> page = movieRepository.findInactiveForAdmin(tmdbFilter, likeKeyword, pageable);
        return mapMoviePage(page, AdminMovieScreeningPhase.INACTIVE, lang);
    }

    /** Hết chiếu tại rạp — từng có suất, không còn suất tương lai. */
    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listEndedAtCinema(String keyword, Pageable pageable, AppLanguage lang) {
        Long tmdbFilter = parseNumericQuery(keyword);
        String likeKeyword = toLikeKeyword(keyword, tmdbFilter);
        Page<Movie> page = movieRepository.findActiveEndedAtCinema(
                LocalDateTime.now(), UPCOMING_STATUSES, tmdbFilter, likeKeyword, pageable);
        return mapMoviePage(page, AdminMovieScreeningPhase.ENDED, lang);
    }

    @Transactional
    public Map<Long, String> resolvePosterUrls(List<Long> movieIds, AppLanguage lang) {
        if (movieIds == null || movieIds.isEmpty()) {
            return Map.of();
        }
        AppLanguage effective = lang != null ? lang : AppLanguage.VI_VN;
        Map<Long, String> result = new LinkedHashMap<>();
        for (Movie movie : movieRepository.findAllById(movieIds)) {
            String url = resolvePosterUrl(movie, effective);
            if (url != null) {
                result.put(movie.getMovieId(), url);
            }
        }
        return result;
    }

    private Page<AdminMovieListItemDto> mapMoviePage(Page<Movie> page, AdminMovieScreeningPhase phase, AppLanguage lang) {
        List<Long> ids = page.getContent().stream().map(Movie::getMovieId).toList();
        Map<Long, MovieShowtimeStats> statsMap = loadStats(ids);
        Map<Long, Long> audienceBookingsMap = loadAudienceBookings(ids);
        List<AdminMovieListItemDto> items = new ArrayList<>();
        for (Movie movie : page.getContent()) {
            MovieShowtimeStats stats = statsMap.get(movie.getMovieId());
            long audienceBookings = audienceBookingsMap.getOrDefault(movie.getMovieId(), 0L);
            String title = movieDisplayService.resolveTitleLocal(movie);
            items.add(toListItem(movie, title, phase, stats, audienceBookings));
        }
        return new PageImpl<>(items, page.getPageable(), page.getTotalElements());
    }

    private String toLikeKeyword(String keyword, Long tmdbFilter) {
        if (tmdbFilter != null || keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    @Transactional(readOnly = true)
    public AdminMoviePageResponse toPageResponse(Page<AdminMovieListItemDto> page) {
        return AdminMoviePageResponse.builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listForAdmin(AdminMovieScreeningPhase phaseFilter,
                                                    MovieStatus statusFilter,
                                                    String keyword,
                                                    Pageable pageable,
                                                    AppLanguage lang) {
        if (phaseFilter == AdminMovieScreeningPhase.WAITING_SCHEDULE
                || phaseFilter == AdminMovieScreeningPhase.HAS_SCHEDULE
                || phaseFilter == AdminMovieScreeningPhase.ENDED) {
            return listSection(phaseFilter, keyword, pageable, lang);
        }
        if (phaseFilter == AdminMovieScreeningPhase.INACTIVE) {
            return listInactive(keyword, pageable, lang);
        }
        Long tmdbFilter = parseNumericQuery(keyword);
        MovieStatus effectiveStatus = statusFilter != null ? statusFilter : null;
        Page<Movie> page = movieRepository.findForAdmin(effectiveStatus, tmdbFilter, pageable);
        List<Long> ids = page.getContent().stream().map(Movie::getMovieId).toList();
        Map<Long, MovieShowtimeStats> statsMap = loadStats(ids);
        Map<Long, Long> audienceBookingsMap = loadAudienceBookings(ids);
        List<AdminMovieListItemDto> items = new ArrayList<>();
        for (Movie movie : page.getContent()) {
            MovieShowtimeStats stats = statsMap.get(movie.getMovieId());
            long audienceBookings = audienceBookingsMap.getOrDefault(movie.getMovieId(), 0L);
            AdminMovieScreeningPhase phase = resolvePhase(movie.getStatus(), stats);
            items.add(toListItem(movie, movieDisplayService.resolveTitleLocal(movie), phase, stats, audienceBookings));
        }
        return new PageImpl<>(items, page.getPageable(), page.getTotalElements());
    }

    private AdminMovieListItemDto toListItem(Movie movie,
                                             String title,
                                             AdminMovieScreeningPhase phase,
                                             MovieShowtimeStats stats,
                                             long audienceBookings) {
        long total = stats != null ? stats.totalCount() : 0;
        long upcoming = stats != null ? stats.upcomingCount() : 0;
        return AdminMovieListItemDto.builder()
                .movieId(movie.getMovieId())
                .tmdbId(movie.getTmdbId())
                .duration(movie.getDuration())
                .ageLabel(movie.getAgeLabel())
                .status(movie.getStatus())
                .publishedAt(movie.getPublishedAt())
                .defaultBasePrice(movie.getDefaultBasePrice())
                .displayTitle(title)
                .posterUrl(posterUrlFromDb(movie))
                .phase(phase)
                .totalShowtimes(total)
                .upcomingShowtimes(upcoming)
                .audienceBookings(audienceBookings)
                .canDeactivate(audienceBookings == 0)
                .nextShowtimeAt(stats != null ? stats.nextStartTime() : null)
                .lastShowtimeAt(stats != null ? stats.lastStartTime() : null)
                .build();
    }

    private Map<Long, Long> loadAudienceBookings(List<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (int i = 0; i < movieIds.size(); i += STATS_BATCH_SIZE) {
            int end = Math.min(i + STATS_BATCH_SIZE, movieIds.size());
            List<Long> batch = movieIds.subList(i, end);
            for (MovieAudienceBookingRow row : bookingRepository.countAudienceBookingsByMovieIds(
                    batch, AUDIENCE_BOOKING_STATUSES)) {
                map.put(row.getMovieId(), row.getBookingCount() != null ? row.getBookingCount() : 0L);
            }
        }
        return map;
    }

    private String posterUrlFromDb(Movie movie) {
        if (movie.getPosterPath() == null || movie.getPosterPath().isBlank()) {
            return null;
        }
        return tmdbImageUrlBuilder.posterW500(movie.getPosterPath());
    }

    /** Đọc DB; nếu thiếu poster_path thì lấy TMDB một lần và lưu lại. */
    private String resolvePosterUrl(Movie movie, AppLanguage lang) {
        String cached = posterUrlFromDb(movie);
        if (cached != null || movie.getTmdbId() == null) {
            return cached;
        }
        try {
            MovieCatalogDetailDto catalog = tmdbCatalogService.getDetail(lang, movie.getTmdbId());
            String path = trimPosterPath(catalog.getPosterPath());
            if (path == null) {
                return null;
            }
            movie.setPosterPath(path);
            if (movie.getDisplayTitleVi() == null || movie.getDisplayTitleVi().isBlank()) {
                movie.setDisplayTitleVi(resolveDisplayTitle(catalog));
            }
            movieRepository.save(movie);
            return tmdbImageUrlBuilder.posterW500(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String trimPosterPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.trim();
    }

    private static String resolveDisplayTitle(MovieCatalogDetailDto catalog) {
        if (catalog.getTitle() != null && !catalog.getTitle().isBlank()) {
            return catalog.getTitle().trim();
        }
        if (catalog.getOriginalTitle() != null && !catalog.getOriginalTitle().isBlank()) {
            return catalog.getOriginalTitle().trim();
        }
        return catalog.getTmdbId() != null ? "TMDB #" + catalog.getTmdbId() : "Phim";
    }

    private Map<Long, MovieShowtimeStats> loadStats(List<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return Map.of();
        }
        LocalDateTime now = LocalDateTime.now();
        Map<Long, MovieShowtimeStats> map = new HashMap<>();
        for (int i = 0; i < movieIds.size(); i += STATS_BATCH_SIZE) {
            int end = Math.min(i + STATS_BATCH_SIZE, movieIds.size());
            List<Long> batch = movieIds.subList(i, end);
            List<MovieShowtimeStatsRow> rows = showtimeRepository.findShowtimeStatsByMovieIds(
                    batch, now, UPCOMING_STATUSES);
            for (MovieShowtimeStatsRow row : rows) {
                map.put(row.getMovieId(), new MovieShowtimeStats(
                        row.getMovieId(),
                        row.getTotalCount() != null ? row.getTotalCount() : 0L,
                        row.getUpcomingCount() != null ? row.getUpcomingCount() : 0L,
                        row.getNextStartTime(),
                        row.getLastStartTime()));
            }
        }
        return map;
    }

    static AdminMovieScreeningPhase resolvePhase(MovieStatus status, MovieShowtimeStats stats) {
        if (status == MovieStatus.INACTIVE) {
            return AdminMovieScreeningPhase.INACTIVE;
        }
        if (stats == null || stats.totalCount() == 0) {
            return AdminMovieScreeningPhase.WAITING_SCHEDULE;
        }
        if (stats.upcomingCount() > 0) {
            return AdminMovieScreeningPhase.HAS_SCHEDULE;
        }
        return AdminMovieScreeningPhase.ENDED;
    }

    private Long parseNumericQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(q.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
