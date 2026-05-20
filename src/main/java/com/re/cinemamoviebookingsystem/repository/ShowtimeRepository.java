package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Showtime;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.repository.projection.MovieShowtimeStatsRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    @Query("""
            SELECT COUNT(s) > 0 FROM Showtime s
            WHERE s.room.roomId = :roomId
            AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED
            AND s.startTime < :endTime AND s.endTime > :startTime
            AND (:excludeId IS NULL OR s.showtimeId <> :excludeId)
            """)
    boolean existsRoomConflict(@Param("roomId") Integer roomId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("excludeId") Long excludeId);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.room r
            WHERE s.status IN :statuses
            AND s.startTime > :now
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findUpcoming(@Param("now") LocalDateTime now,
                                @Param("statuses") List<ShowtimeStatus> statuses);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.room r
            WHERE s.status IN :statuses
            AND m.status = com.re.cinemamoviebookingsystem.enums.MovieStatus.ACTIVE
            AND s.startTime >= :from
            AND s.startTime < :to
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findScheduleWindow(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("statuses") List<ShowtimeStatus> statuses);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.room r
            WHERE m.tmdbId = :tmdbId
            AND s.status IN :statuses
            AND s.startTime > :now
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findUpcomingByTmdbId(@Param("tmdbId") long tmdbId,
                                        @Param("now") LocalDateTime now,
                                        @Param("statuses") List<ShowtimeStatus> statuses);

    List<Showtime> findByMovieMovieIdAndStatusNot(Long movieId, ShowtimeStatus status);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.room r
            WHERE m.movieId = :movieId
            ORDER BY s.startTime DESC
            """)
    List<Showtime> findByMovieIdWithDetails(@Param("movieId") Long movieId);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.room r
            WHERE s.startTime >= :from AND s.startTime < :to
            AND (:roomId IS NULL OR r.roomId = :roomId)
            ORDER BY r.roomName, s.startTime
            """)
    List<Showtime> findForCalendar(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   @Param("roomId") Integer roomId);

    @Query("""
            SELECT COUNT(s) FROM Showtime s
            WHERE s.startTime >= :start AND s.startTime < :end
            AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED
            """)
    long countBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.room r
            WHERE (:movieId IS NULL OR m.movieId = :movieId)
            AND (:roomId IS NULL OR r.roomId = :roomId)
            AND (:status IS NULL OR s.status = :status)
            AND (:from IS NULL OR s.startTime >= :from)
            AND (:to IS NULL OR s.startTime <= :to)
            ORDER BY s.startTime DESC
            """)
    Page<Showtime> findForAdmin(@Param("movieId") Long movieId,
                                @Param("roomId") Integer roomId,
                                @Param("status") ShowtimeStatus status,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                Pageable pageable);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.room r
            WHERE s.showtimeId = :id
            """)
    java.util.Optional<Showtime> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT s FROM Showtime s
            JOIN FETCH s.movie m
            JOIN FETCH s.room r
            WHERE s.status IN :statuses
            AND s.startTime > :now
            AND NOT EXISTS (
                SELECT 1 FROM ShowtimeSeat ss WHERE ss.showtime = s
            )
            ORDER BY s.startTime ASC
            """)
    List<Showtime> findUpcomingWithoutSeatRows(@Param("now") LocalDateTime now,
                                               @Param("statuses") List<ShowtimeStatus> statuses);

    @Query("""
            SELECT m.movieId AS movieId,
                   COUNT(s) AS totalCount,
                   SUM(CASE WHEN s.startTime > :now AND s.status IN :upcomingStatuses THEN 1 ELSE 0 END) AS upcomingCount,
                   MIN(CASE WHEN s.startTime > :now AND s.status IN :upcomingStatuses THEN s.startTime ELSE NULL END) AS nextStartTime,
                   MAX(s.startTime) AS lastStartTime
            FROM Showtime s
            JOIN s.movie m
            WHERE m.movieId IN :movieIds
            AND s.status <> com.re.cinemamoviebookingsystem.enums.ShowtimeStatus.CANCELLED
            GROUP BY m.movieId
            """)
    List<MovieShowtimeStatsRow> findShowtimeStatsByMovieIds(@Param("movieIds") List<Long> movieIds,
                                                            @Param("now") LocalDateTime now,
                                                            @Param("upcomingStatuses") List<ShowtimeStatus> upcomingStatuses);
}
