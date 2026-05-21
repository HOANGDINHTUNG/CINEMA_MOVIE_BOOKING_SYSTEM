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

    @Query("""
            SELECT m FROM Movie m
            WHERE m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.ACTIVE
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(m.displayTitleVi, '')) LIKE :keyword
                OR LOWER(COALESCE(m.adminNote, '')) LIKE :keyword)
            AND NOT EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m
                AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED)
            ORDER BY m.publishedAt DESC, m.movieId DESC
            """)
    Page<Movie> findActiveWaitingSchedule(@Param("tmdbId") Long tmdbId,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    @Query("""
            SELECT m FROM Movie m
            WHERE m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.ACTIVE
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(m.displayTitleVi, '')) LIKE :keyword
                OR LOWER(COALESCE(m.adminNote, '')) LIKE :keyword)
            AND EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m
                AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED
                AND s.startTime > :now
                AND s.status IN :upcomingStatuses)
            ORDER BY m.publishedAt DESC, m.movieId DESC
            """)
    Page<Movie> findActiveHasUpcomingSchedule(@Param("now") LocalDateTime now,
                                              @Param("upcomingStatuses") List<ShowtimeStatus> upcomingStatuses,
                                              @Param("tmdbId") Long tmdbId,
                                              @Param("keyword") String keyword,
                                              Pageable pageable);

    @Query("""
            SELECT m FROM Movie m
            WHERE m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.ACTIVE
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(m.displayTitleVi, '')) LIKE :keyword
                OR LOWER(COALESCE(m.adminNote, '')) LIKE :keyword)
            AND EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m
                AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED)
            AND NOT EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m
                AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED
                AND s.startTime > :now
                AND s.status IN :upcomingStatuses)
            ORDER BY m.publishedAt DESC, m.movieId DESC
            """)
    Page<Movie> findActiveEndedAtCinema(@Param("now") LocalDateTime now,
                                        @Param("upcomingStatuses") List<ShowtimeStatus> upcomingStatuses,
                                        @Param("tmdbId") Long tmdbId,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    @Query("""
            SELECT COUNT(m) FROM Movie m
            WHERE m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.ACTIVE
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(m.displayTitleVi, '')) LIKE :keyword
                OR LOWER(COALESCE(m.adminNote, '')) LIKE :keyword)
            AND NOT EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m
                AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED)
            """)
    long countActiveWaitingSchedule(@Param("tmdbId") Long tmdbId, @Param("keyword") String keyword);

    @Query("""
            SELECT COUNT(m) FROM Movie m
            WHERE m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.ACTIVE
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(m.displayTitleVi, '')) LIKE :keyword
                OR LOWER(COALESCE(m.adminNote, '')) LIKE :keyword)
            AND EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m
                AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED
                AND s.startTime > :now
                AND s.status IN :upcomingStatuses)
            """)
    long countActiveHasUpcomingSchedule(@Param("now") LocalDateTime now,
                                        @Param("upcomingStatuses") List<ShowtimeStatus> upcomingStatuses,
                                        @Param("tmdbId") Long tmdbId,
                                        @Param("keyword") String keyword);

    @Query("""
            SELECT COUNT(m) FROM Movie m
            WHERE m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.ACTIVE
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(m.displayTitleVi, '')) LIKE :keyword
                OR LOWER(COALESCE(m.adminNote, '')) LIKE :keyword)
            AND EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m
                AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED)
            AND NOT EXISTS (
                SELECT 1 FROM Showtime s
                WHERE s.movie = m
                AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED
                AND s.startTime > :now
                AND s.status IN :upcomingStatuses)
            """)
    long countActiveEndedAtCinema(@Param("now") LocalDateTime now,
                                  @Param("upcomingStatuses") List<ShowtimeStatus> upcomingStatuses,
                                  @Param("tmdbId") Long tmdbId,
                                  @Param("keyword") String keyword);

    @Query("""
            SELECT COUNT(m) FROM Movie m
            WHERE m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.INACTIVE
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(m.displayTitleVi, '')) LIKE :keyword
                OR LOWER(COALESCE(m.adminNote, '')) LIKE :keyword)
            """)
    long countInactiveForAdmin(@Param("tmdbId") Long tmdbId, @Param("keyword") String keyword);

    @Query("""
            SELECT m FROM Movie m
            WHERE m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.INACTIVE
            AND (:tmdbId IS NULL OR m.tmdbId = :tmdbId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(m.displayTitleVi, '')) LIKE :keyword
                OR LOWER(COALESCE(m.adminNote, '')) LIKE :keyword)
            ORDER BY m.unpublishedAt DESC, m.publishedAt DESC, m.movieId DESC
            """)
    Page<Movie> findInactiveForAdmin(@Param("tmdbId") Long tmdbId,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}
