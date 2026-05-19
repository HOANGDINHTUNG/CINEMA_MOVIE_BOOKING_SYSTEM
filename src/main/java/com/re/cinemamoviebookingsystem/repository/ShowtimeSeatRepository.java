package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.ShowtimeSeat;
import com.re.cinemamoviebookingsystem.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {

    List<ShowtimeSeat> findByShowtimeShowtimeId(Long showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ss FROM ShowtimeSeat ss
            JOIN FETCH ss.seat
            WHERE ss.showtime.showtimeId = :showtimeId
            AND ss.seat.seatId IN :seatIds
            """)
    List<ShowtimeSeat> findByShowtimeAndSeatIdsForUpdate(@Param("showtimeId") Long showtimeId,
                                                         @Param("seatIds") List<Long> seatIds);

    long countByShowtimeShowtimeIdAndStatus(Long showtimeId, SeatStatus status);

    Optional<ShowtimeSeat> findByShowtimeShowtimeIdAndSeatSeatId(Long showtimeId, Long seatId);

    @Query("""
            SELECT ss FROM ShowtimeSeat ss
            WHERE ss.status = com.re.cinemamoviebookingsystem.enums.SeatStatus.LOCKED
            AND ss.lockedUntil IS NOT NULL AND ss.lockedUntil < :now
            """)
    List<ShowtimeSeat> findExpiredLocks(@Param("now") LocalDateTime now);
}
