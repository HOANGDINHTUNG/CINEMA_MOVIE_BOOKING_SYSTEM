package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
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
            SELECT m.title, SUM(b.total_amount) as revenue
            FROM bookings b
            JOIN showtimes st ON b.showtime_id = st.showtime_id
            JOIN movies m ON st.movie_id = m.movie_id
            WHERE b.status = 'PAID'
            GROUP BY m.movie_id, m.title
            ORDER BY revenue DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> topMoviesByRevenue();

    List<Booking> findByStatusAndBookingDateBefore(BookingStatus status, LocalDateTime before);

    long countByShowtimeShowtimeIdAndStatus(Long showtimeId, BookingStatus status);
}
