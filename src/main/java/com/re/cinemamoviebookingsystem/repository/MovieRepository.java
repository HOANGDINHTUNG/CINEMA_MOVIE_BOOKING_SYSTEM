package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Movie;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    long countByStatus(MovieStatus status);

    List<Movie> findByStatusOrderByPublishedAtDesc(MovieStatus status);

    Optional<Movie> findByTmdbId(Long tmdbId);

    boolean existsByTmdbId(Long tmdbId);

    @Query("""
            SELECT m FROM Movie m
            WHERE (:status IS NULL OR m.status = :status)
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            ORDER BY m.publishedAt DESC, m.movieId DESC
            """)
    Page<Movie> findForAdmin(@Param("status") MovieStatus status,
                             @Param("tmdbId") Long tmdbId,
                             Pageable pageable);

    @Query("""
            SELECT DISTINCT m FROM Movie m
            JOIN Showtime s ON s.movie = m
            WHERE m.status = :status
            AND s.startTime > :now
            AND s.status IN :showtimeStatuses
            ORDER BY m.publishedAt DESC
            """)
    List<Movie> findActiveWithUpcomingShowtimes(@Param("status") MovieStatus status,
                                                @Param("now") LocalDateTime now,
                                                @Param("showtimeStatuses") List<ShowtimeStatus> showtimeStatuses);
}
