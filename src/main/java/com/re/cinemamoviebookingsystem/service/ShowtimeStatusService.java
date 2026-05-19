package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Showtime;
import com.re.cinemamoviebookingsystem.enums.SeatStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.repository.ShowtimeRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShowtimeStatusService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    @Transactional(rollbackFor = Exception.class)
    public void refreshShowtimeStatus(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        if (showtime == null || showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            return;
        }
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            showtime.setStatus(ShowtimeStatus.HIDDEN);
            showtimeRepository.save(showtime);
            return;
        }
        long booked = showtimeSeatRepository.countByShowtimeShowtimeIdAndStatus(showtimeId, SeatStatus.BOOKED);
        int total = showtime.getRoom().getTotalSeats();
        if (booked >= total) {
            showtime.setStatus(ShowtimeStatus.SOLD_OUT);
        } else if (showtime.getStatus() == ShowtimeStatus.SOLD_OUT) {
            showtime.setStatus(ShowtimeStatus.ACTIVE);
        }
        showtimeRepository.save(showtime);
    }
}
