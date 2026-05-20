package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room
            LEFT JOIN FETCH b.tickets t
            LEFT JOIN FETCH t.seat
            WHERE b.user.userId = :userId
            ORDER BY b.bookingDate DESC
            """)
    List<Booking> findByUserWithDetails(@Param("userId") Long userId);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user u
            JOIN FETCH u.profile
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room
            LEFT JOIN FETCH b.tickets t
            LEFT JOIN FETCH t.seat
            WHERE b.bookingId = :bookingId
            """)
    Optional<Booking> findByIdWithDetails(@Param("bookingId") Long bookingId);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b
            WHERE b.status = com.re.cinemamoviebookingsystem.enums.BookingStatus.PAID
            AND b.bookingDate BETWEEN :from AND :to
            """)
    BigDecimal sumRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT MONTH(b.booking_date) as m, SUM(b.total_amount) as revenue
            FROM bookings b
            WHERE b.status = 'PAID' AND YEAR(b.booking_date) = :year
            GROUP BY MONTH(b.booking_date)
            ORDER BY m
            """, nativeQuery = true)
    List<Object[]> revenueByMonth(@Param("year") int year);

    @Query(value = """
            SELECT m.movie_id, m.tmdb_id, SUM(b.total_amount) as revenue
            FROM bookings b
            JOIN showtimes st ON b.showtime_id = st.showtime_id
            JOIN movies m ON st.movie_id = m.movie_id
            WHERE b.status = 'PAID'
            GROUP BY m.movie_id, m.tmdb_id
            ORDER BY revenue DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> topMoviesByRevenue();

    List<Booking> findByStatusAndBookingDateBefore(BookingStatus status, LocalDateTime before);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user u
            LEFT JOIN FETCH u.profile
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room
            WHERE b.status = :status AND b.bookingDate < :before
            ORDER BY b.bookingDate ASC
            """)
    List<Booking> findStalePendingWithDetails(@Param("status") BookingStatus status,
                                              @Param("before") LocalDateTime before);

    long countByShowtimeShowtimeIdAndStatus(Long showtimeId, BookingStatus status);

    long countByStatus(BookingStatus status);

    @Query(value = """
            SELECT DATE(b.booking_date) as d, COALESCE(SUM(b.total_amount), 0) as revenue
            FROM bookings b
            WHERE b.status = 'PAID' AND b.booking_date >= :from
            GROUP BY DATE(b.booking_date)
            ORDER BY d
            """, nativeQuery = true)
    List<Object[]> revenueByDay(@Param("from") LocalDateTime from);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user u
            LEFT JOIN FETCH u.profile
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room
            WHERE (:status IS NULL OR b.status = :status)
            AND (:bookingId IS NULL OR b.bookingId = :bookingId)
            ORDER BY b.bookingDate DESC
            """)
    Page<Booking> findForAdmin(@Param("status") BookingStatus status,
                               @Param("bookingId") Long bookingId,
                               Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user u
            LEFT JOIN FETCH u.profile
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room
            WHERE b.status = :status
            ORDER BY b.bookingDate ASC
            """)
    List<Booking> findTopByStatusOrderByBookingDateAsc(@Param("status") BookingStatus status,
                                                       Pageable pageable);

    long countByStatusAndBookingDateBetween(BookingStatus status,
                                            LocalDateTime from,
                                            LocalDateTime to);

    @Query(value = """
            SELECT r.room_name, SUM(b.total_amount) as revenue, COUNT(b.booking_id) as cnt
            FROM bookings b
            JOIN showtimes st ON b.showtime_id = st.showtime_id
            JOIN rooms r ON st.room_id = r.room_id
            WHERE b.status = 'PAID'
            AND b.booking_date BETWEEN :from AND :to
            GROUP BY r.room_id, r.room_name
            ORDER BY revenue DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> topRoomsByRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT m.movie_id, m.tmdb_id, SUM(b.total_amount) as revenue
            FROM bookings b
            JOIN showtimes st ON b.showtime_id = st.showtime_id
            JOIN movies m ON st.movie_id = m.movie_id
            WHERE b.status = 'PAID'
            AND b.booking_date BETWEEN :from AND :to
            GROUP BY m.movie_id, m.tmdb_id
            ORDER BY revenue DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> topMoviesByRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user u
            LEFT JOIN FETCH u.profile
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room
            WHERE b.bookingDate BETWEEN :from AND :to
            AND (:status IS NULL OR b.status = :status)
            ORDER BY b.bookingDate DESC
            """)
    List<Booking> findForExport(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                @Param("status") BookingStatus status);

    Optional<Booking> findByUser_UserIdAndShowtime_ShowtimeIdAndStatus(Long userId,
                                                                       Long showtimeId,
                                                                       BookingStatus status);

    List<Booking> findByStatus(BookingStatus status);
}
