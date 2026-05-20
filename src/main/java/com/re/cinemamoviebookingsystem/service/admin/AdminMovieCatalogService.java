package com.re.cinemamoviebookingsystem.service.admin;

import com.re.cinemamoviebookingsystem.dto.response.AdminMovieListItemDto;
import com.re.cinemamoviebookingsystem.dto.response.AdminMoviePageResponse;
import com.re.cinemamoviebookingsystem.dto.response.AdminMoviePhaseCountsDto;
import com.re.cinemamoviebookingsystem.entity.Movie;
import com.re.cinemamoviebookingsystem.enums.AdminMovieScreeningPhase;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.repository.MovieRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeRepository;
import com.re.cinemamoviebookingsystem.repository.projection.MovieShowtimeStatsRow;
import com.re.cinemamoviebookingsystem.service.MovieDisplayService;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.tmdb.service.TmdbCatalogService;
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

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieDisplayService movieDisplayService;
    private final TmdbCatalogService tmdbCatalogService;

    @Transactional(readOnly = true)
    public AdminMoviePhaseCountsDto countByPhase() {
        List<Movie> movies = movieRepository.findAll(Sort.by(Sort.Direction.DESC, "publishedAt"));
        Map<Long, MovieShowtimeStats> statsMap = loadStats(movies.stream().map(Movie::getMovieId).toList());
        long hasSchedule = 0;
        long waitingSchedule = 0;
        long ended = 0;
        long inactive = 0;
        for (Movie movie : movies) {
            AdminMovieScreeningPhase phase = resolvePhase(movie.getStatus(), statsMap.get(movie.getMovieId()));
            switch (phase) {
                case HAS_SCHEDULE -> hasSchedule++;
                case WAITING_SCHEDULE -> waitingSchedule++;
                case ENDED -> ended++;
                case INACTIVE -> inactive++;
            }
        }
        return AdminMoviePhaseCountsDto.builder()
                .hasSchedule(hasSchedule)
                .waitingSchedule(waitingSchedule)
                .ended(ended)
                .inactive(inactive)
                .total(movies.size())
                .build();
    }

    /** Đã có lịch chiếu tại rạp (có suất sắp tới). */
    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listHasSchedule(String keyword, Pageable pageable, AppLanguage lang) {
        return listForAdmin(AdminMovieScreeningPhase.HAS_SCHEDULE, MovieStatus.ACTIVE, keyword, pageable, lang);
    }

    /** Đang đợi xếp lịch — đã đăng rạp, chưa có suất. */
    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listWaitingSchedule(String keyword, Pageable pageable, AppLanguage lang) {
        return listForAdmin(AdminMovieScreeningPhase.WAITING_SCHEDULE, MovieStatus.ACTIVE, keyword, pageable, lang);
    }

    /** Hết chiếu tại rạp — từng có suất, không còn suất tương lai. */
    @Transactional(readOnly = true)
    public Page<AdminMovieListItemDto> listEndedAtCinema(String keyword, Pageable pageable, AppLanguage lang) {
        return listForAdmin(AdminMovieScreeningPhase.ENDED, MovieStatus.ACTIVE, keyword, pageable, lang);
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
        AppLanguage effective = lang != null ? lang : AppLanguage.VI_VN;
        Long tmdbFilter = parseNumericQuery(keyword);
        List<Movie> candidates = loadCandidates(statusFilter, tmdbFilter);
        Map<Long, MovieShowtimeStats> statsMap = loadStats(candidates.stream().map(Movie::getMovieId).toList());

        List<AdminMovieListItemDto> items = new ArrayList<>();
        for (Movie movie : candidates) {
            MovieShowtimeStats stats = statsMap.get(movie.getMovieId());
            AdminMovieScreeningPhase phase = resolvePhase(movie.getStatus(), stats);
            if (phaseFilter != null && phase != phaseFilter) {
                continue;
            }
            String title = movieDisplayService.resolveTitle(movie, effective);
            if (keyword != null && !keyword.isBlank() && tmdbFilter == null) {
                String lower = keyword.trim().toLowerCase();
                if (title == null || !title.toLowerCase().contains(lower)) {
                    continue;
                }
            }
            items.add(toListItem(movie, title, phase, stats, effective, false));
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<AdminMovieListItemDto> pageContent = start >= items.size()
                ? List.of()
                : items.subList(start, end);
        return new PageImpl<>(pageContent, pageable, items.size());
    }

    private List<Movie> loadCandidates(MovieStatus statusFilter, Long tmdbFilter) {
        if (tmdbFilter != null) {
            return movieRepository.findForAdmin(statusFilter, tmdbFilter, Pageable.unpaged()).getContent();
        }
        return movieRepository.findForAdmin(statusFilter, null, Pageable.unpaged(
                Sort.by(Sort.Direction.DESC, "publishedAt", "movieId"))).getContent();
    }

    private AdminMovieListItemDto toListItem(Movie movie,
                                             String title,
                                             AdminMovieScreeningPhase phase,
                                             MovieShowtimeStats stats,
                                             AppLanguage lang,
                                             boolean loadPoster) {
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
                .posterUrl(loadPoster ? resolvePosterUrl(movie, lang) : null)
                .phase(phase)
                .totalShowtimes(total)
                .upcomingShowtimes(upcoming)
                .nextShowtimeAt(stats != null ? stats.nextStartTime() : null)
                .lastShowtimeAt(stats != null ? stats.lastStartTime() : null)
                .build();
    }

    private String resolvePosterUrl(Movie movie, AppLanguage lang) {
        if (movie.getTmdbId() == null) {
            return null;
        }
        try {
            var detail = tmdbCatalogService.getDetail(lang, movie.getTmdbId());
            String url = detail.getPosterUrl();
            return (url != null && !url.isBlank()) ? url : null;
        } catch (Exception ignored) {
            return null;
        }
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
