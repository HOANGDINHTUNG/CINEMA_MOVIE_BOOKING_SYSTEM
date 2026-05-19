package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByRoomRoomId(Integer roomId);
    long countByRoomRoomId(Integer roomId);
}
