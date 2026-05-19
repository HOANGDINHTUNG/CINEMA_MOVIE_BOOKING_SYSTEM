package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.MovieDto;
import com.re.cinemamoviebookingsystem.dto.response.catalog.MovieCatalogDetailDto;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CinemaMovieService {

    private final MovieRepository movieRepository;
    private final MovieService movieService;
    private final TmdbCatalogService tmdbCatalogService;
    private final ShowtimeScheduleService showtimeScheduleService;

    @Transactional(rollbackFor = Exception.class)
    public MovieDto publishToCinema(long tmdbId, AppLanguage lang, BigDecimal defaultBasePrice) {
        return publishToCinema(tmdbId, lang, defaultBasePrice, true);
    }

    /**
     * @param generateSchedule false = chỉ đăng rạp (hiện ở «sắp chiếu»), true = tạo lịch mẫu («đang chiếu»).
     */
    @Transactional(rollbackFor = Exception.class)
    public MovieDto publishToCinema(long tmdbId, AppLanguage lang, BigDecimal defaultBasePrice,
                                    boolean generateSchedule) {
        if (movieRepository.existsByTmdbId(tmdbId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Phim TMDB #" + tmdbId + " đã được đăng chiếu tại rạp");
        }

        MovieCatalogDetailDto catalog = tmdbCatalogService.getDetail(lang, tmdbId);
        String titleForAge = catalog.getTitle() != null ? catalog.getTitle() : catalog.getOriginalTitle();
        BigDecimal base = defaultBasePrice != null ? defaultBasePrice : new BigDecimal("85000");

        Movie movie = Movie.builder()
                .tmdbId(tmdbId)
                .duration(resolveRuntime(catalog.getRuntime()))
                .ageLabel(resolveAgeLabel(catalog, titleForAge))
                .status(MovieStatus.ACTIVE)
                .publishedAt(LocalDateTime.now())
                .defaultBasePrice(base)
                .runtimeSyncedAt(LocalDateTime.now())
                .build();
        movie = movieRepository.save(movie);

        if (generateSchedule) {
            showtimeScheduleService.generateInitialSchedule(movie.getMovieId());
        }
        return movieService.toDto(movie, lang);
    }

    /** @deprecated Dùng {@link #publishToCinema(long, AppLanguage, BigDecimal)} */
    @Transactional(rollbackFor = Exception.class)
    public MovieDto importFromTmdb(long tmdbId, AppLanguage lang) {
        return publishToCinema(tmdbId, lang, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public MovieDto refreshRuntimeFromTmdb(Long movieId, AppLanguage lang) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phim không tồn tại"));
        if (movie.getTmdbId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Phim chưa liên kết TMDB");
        }
        MovieCatalogDetailDto catalog = tmdbCatalogService.getDetail(lang, movie.getTmdbId());
        if (catalog.getRuntime() != null && catalog.getRuntime() > 0) {
            movie.setDuration(catalog.getRuntime());
        }
        String title = catalog.getTitle() != null ? catalog.getTitle() : catalog.getOriginalTitle();
        movie.setAgeLabel(resolveAgeLabel(catalog, title));
        movie.setRuntimeSyncedAt(LocalDateTime.now());
        return movieService.toDto(movieRepository.save(movie), lang);
    }

    @Transactional(readOnly = true)
    public MovieDto findByTmdbId(long tmdbId, AppLanguage lang) {
        Movie movie = movieRepository.findByTmdbId(tmdbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST,
                        "Chưa có phim rạp với tmdbId=" + tmdbId));
        return movieService.toDto(movie, lang);
    }

    @Transactional(readOnly = true)
    public Optional<MovieDto> findOptionalByTmdbId(long tmdbId, AppLanguage lang) {
        return movieRepository.findByTmdbId(tmdbId).map(m -> movieService.toDto(m, lang));
    }

    @Transactional(readOnly = true)
    public boolean isPublishedAndActive(long tmdbId) {
        return movieRepository.findByTmdbId(tmdbId)
                .map(m -> m.getStatus() == MovieStatus.ACTIVE)
                .orElse(false);
    }

    private static int resolveRuntime(Integer runtime) {
        if (runtime == null || runtime <= 0) {
            return 120;
        }
        return runtime;
    }

    private static String resolveAgeLabel(MovieCatalogDetailDto catalog, String title) {
        if (catalog.getAgeLabel() != null && !catalog.getAgeLabel().isBlank()) {
            return catalog.getAgeLabel();
        }
        return MovieAgeUtil.extractAgeLabel(title);
    }
}
