package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Showtime;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
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

    List<Showtime> findByMovieMovieIdAndStatusNot(Long movieId, ShowtimeStatus status);
}
