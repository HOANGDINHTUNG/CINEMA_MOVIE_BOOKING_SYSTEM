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

    void deleteByShowtimeShowtimeId(Long showtimeId);

    long countByShowtimeShowtimeId(Long showtimeId);

    @Query("""
            SELECT ss FROM ShowtimeSeat ss
            JOIN FETCH ss.seat seat
            JOIN FETCH ss.showtime st
            WHERE st.showtimeId = :showtimeId
            ORDER BY seat.rowName ASC, seat.seatNumber ASC
            """)
    List<ShowtimeSeat> findByShowtimeIdWithSeats(@Param("showtimeId") Long showtimeId);

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

    @Query("""
            SELECT ss FROM ShowtimeSeat ss
            JOIN FETCH ss.seat
            WHERE ss.showtime.showtimeId = :showtimeId
            AND ss.status = com.re.cinemamoviebookingsystem.enums.SeatStatus.LOCKED
            AND ss.lockedByUser.userId = :userId
            AND ss.lockedUntil IS NOT NULL AND ss.lockedUntil > :now
            ORDER BY seat.rowName ASC, seat.seatNumber ASC
            """)
    List<ShowtimeSeat> findActiveLocksByUserAndShowtime(@Param("userId") Long userId,
                                                        @Param("showtimeId") Long showtimeId,
                                                        @Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(ss) > 0 FROM ShowtimeSeat ss
            WHERE ss.showtime.showtimeId = :showtimeId
            AND ss.status = com.re.cinemamoviebookingsystem.enums.SeatStatus.LOCKED
            AND ss.lockedByUser.userId = :userId
            AND ss.lockedUntil IS NOT NULL AND ss.lockedUntil > :now
            """)
    boolean existsActiveLockByUserAndShowtime(@Param("userId") Long userId,
                                              @Param("showtimeId") Long showtimeId,
                                              @Param("now") LocalDateTime now);
}
