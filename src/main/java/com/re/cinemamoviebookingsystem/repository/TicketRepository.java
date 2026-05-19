package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    @Query("""
            SELECT t FROM Ticket t
            JOIN FETCH t.booking b
            JOIN FETCH b.user u
            JOIN FETCH u.profile
            JOIN FETCH b.showtime st
            JOIN FETCH st.movie
            JOIN FETCH st.room
            JOIN FETCH t.seat
            WHERE t.ticketCode = :code
            """)
    Optional<Ticket> findByTicketCodeWithDetails(@Param("code") String code);
}
