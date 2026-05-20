package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.MovieCinemaUpdateRequest;
import com.re.cinemamoviebookingsystem.dto.response.MovieDto;
import com.re.cinemamoviebookingsystem.entity.Movie;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.MovieRepository;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieDisplayService movieDisplayService;

    @Transactional(readOnly = true)
    public Page<MovieDto> listAll(Pageable pageable, AppLanguage lang) {
        return movieRepository.findAll(pageable).map(m -> toDto(m, lang));
    }

    @Transactional(readOnly = true)
    public Page<MovieDto> listForAdmin(MovieStatus status, String q, Pageable pageable, AppLanguage lang) {
        Long tmdbFilter = parseNumericQuery(q);
        Page<MovieDto> page = movieRepository.findForAdmin(status, tmdbFilter, pageable)
                .map(m -> toDto(m, lang));
        if (q == null || q.isBlank() || tmdbFilter != null) {
            return page;
        }
        String lower = q.trim().toLowerCase();
        List<MovieDto> filtered = page.getContent().stream()
                .filter(d -> d.getDisplayTitle() != null
                        && d.getDisplayTitle().toLowerCase().contains(lower))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
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

    @Transactional(readOnly = true)
    public List<MovieDto> listActive(AppLanguage lang) {
        return movieRepository.findByStatusOrderByPublishedAtDesc(MovieStatus.ACTIVE).stream()
                .map(m -> toDto(m, lang))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovieDto> listActiveWithUpcomingShowtimes(AppLanguage lang) {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeStatus> statuses = List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);
        return movieRepository.findActiveWithUpcomingShowtimes(MovieStatus.ACTIVE, now, statuses).stream()
                .map(m -> toDto(m, lang))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> mapTmdbToMovieIds() {
        Map<Long, Long> map = new HashMap<>();
        for (Movie movie : movieRepository.findAll()) {
            if (movie.getTmdbId() != null) {
                map.put(movie.getTmdbId(), movie.getMovieId());
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public MovieDto findById(Long id, AppLanguage lang) {
        return toDto(getMovie(id), lang);
    }

    @Transactional(readOnly = true)
    public MovieDto findById(Long id) {
        return toDto(getMovie(id), AppLanguage.VI_VN);
    }

    @Transactional(rollbackFor = Exception.class)
    public MovieDto updateCinemaFields(Long id, MovieCinemaUpdateRequest request, AppLanguage lang) {
        Movie movie = getMovie(id);
        if (request.getDuration() != null) {
            movie.setDuration(request.getDuration());
        }
        if (request.getAgeLabel() != null) {
            movie.setAgeLabel(request.getAgeLabel().trim());
        }
        if (request.getDefaultBasePrice() != null) {
            movie.setDefaultBasePrice(request.getDefaultBasePrice());
        }
        if (request.getAdminNote() != null) {
            movie.setAdminNote(request.getAdminNote());
        }
        return toDto(movieRepository.save(movie), lang);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deactivate(Long id) {
        Movie movie = getMovie(id);
        movie.setStatus(MovieStatus.INACTIVE);
        movie.setUnpublishedAt(LocalDateTime.now());
        movieRepository.save(movie);
    }

    @Transactional(rollbackFor = Exception.class)
    public void activate(Long id) {
        Movie movie = getMovie(id);
        movie.setStatus(MovieStatus.ACTIVE);
        movie.setUnpublishedAt(null);
        if (movie.getPublishedAt() == null) {
            movie.setPublishedAt(LocalDateTime.now());
        }
        movieRepository.save(movie);
    }

    Movie getMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phim không tồn tại"));
    }

    public MovieDto toDto(Movie movie, AppLanguage lang) {
        AppLanguage effective = lang != null ? lang : AppLanguage.VI_VN;
        return MovieDto.builder()
                .movieId(movie.getMovieId())
                .tmdbId(movie.getTmdbId())
                .duration(movie.getDuration())
                .ageLabel(movie.getAgeLabel())
                .status(movie.getStatus())
                .publishedAt(movie.getPublishedAt())
                .unpublishedAt(movie.getUnpublishedAt())
                .defaultBasePrice(movie.getDefaultBasePrice())
                .adminNote(movie.getAdminNote())
                .displayTitle(movieDisplayService.resolveTitle(movie, effective))
                .build();
    }
}
