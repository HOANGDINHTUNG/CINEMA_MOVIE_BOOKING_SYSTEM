package com.re.cinemamoviebookingsystem;

import com.re.cinemamoviebookingsystem.entity.*;
import com.re.cinemamoviebookingsystem.enums.MovieStatus;
import com.re.cinemamoviebookingsystem.enums.PhysicalSeatType;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.repository.*;
import com.re.cinemamoviebookingsystem.service.ShowtimeSeatRepairService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ShowtimeSeatRepairServiceTest {

    @Autowired
    private ShowtimeSeatRepairService repairService;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private SeatRepository seatRepository;

    @Test
    @Transactional
    void repairShowtimeIfMissing_createsRowsFromRoomSeats() {
        Room room = roomRepository.save(Room.builder().roomName("Test-R").totalSeats(3).build());
        seatRepository.save(Seat.builder().room(room).rowName("A").seatNumber(1)
                .seatType(PhysicalSeatType.STANDARD).build());
        seatRepository.save(Seat.builder().room(room).rowName("A").seatNumber(2)
                .seatType(PhysicalSeatType.STANDARD).build());

        Movie movie = movieRepository.save(Movie.builder().tmdbId(999_001L).duration(90)
                .status(MovieStatus.ACTIVE).adminNote("test: Repair Movie").build());

        Showtime showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .basePrice(BigDecimal.valueOf(85000))
                .status(ShowtimeStatus.ACTIVE)
                .build());

        assertEquals(0, showtimeSeatRepository.countByShowtimeShowtimeId(showtime.getShowtimeId()));

        int added = repairService.repairShowtimeIfMissing(showtime.getShowtimeId());
        assertEquals(2, added);
        assertEquals(2, showtimeSeatRepository.countByShowtimeShowtimeId(showtime.getShowtimeId()));

        int secondRun = repairService.repairShowtimeIfMissing(showtime.getShowtimeId());
        assertEquals(0, secondRun);
        assertTrue(showtimeSeatRepository.countByShowtimeShowtimeId(showtime.getShowtimeId()) >= 2);
    }
}
