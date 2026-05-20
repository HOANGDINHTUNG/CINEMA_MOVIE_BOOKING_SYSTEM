package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Seat;
import com.re.cinemamoviebookingsystem.entity.Showtime;
import com.re.cinemamoviebookingsystem.entity.ShowtimeSeat;
import com.re.cinemamoviebookingsystem.enums.SeatStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.repository.SeatRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đảm bảo mỗi suất chiếu có đủ dòng {@code showtime_seats} (một dòng / ghế trong phòng).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeSeatRepairService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final SeatRepository seatRepository;

    @Transactional(rollbackFor = Exception.class)
    public int repairAllUpcomingMissingSeats() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeStatus> statuses = List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);
        List<Showtime> targets = showtimeRepository.findUpcomingWithoutSeatRows(now, statuses);
        int repairedShowtimes = 0;
        int totalRows = 0;
        for (Showtime showtime : targets) {
            int added = repairShowtimeIfMissing(showtime.getShowtimeId());
            if (added > 0) {
                repairedShowtimes++;
                totalRows += added;
            }
        }
        if (repairedShowtimes > 0) {
            log.info("Showtime seat repair: {} suất, +{} dòng showtime_seats", repairedShowtimes, totalRows);
        }
        return totalRows;
    }

    @Transactional(rollbackFor = Exception.class)
    public int repairShowtimeIfMissing(Long showtimeId) {
        if (showtimeSeatRepository.countByShowtimeShowtimeId(showtimeId) > 0) {
            return 0;
        }
        return rebuildShowtimeSeats(showtimeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int rebuildShowtimeSeats(Long showtimeId) {
        Showtime showtime = showtimeRepository.findByIdWithDetails(showtimeId).orElse(null);
        if (showtime == null) {
            return 0;
        }
        List<Seat> roomSeats = seatRepository.findByRoomRoomId(showtime.getRoom().getRoomId());
        if (roomSeats.isEmpty()) {
            log.warn("Showtime {} — phòng {} chưa có ghế trong DB", showtimeId, showtime.getRoom().getRoomId());
            return 0;
        }
        showtimeSeatRepository.deleteByShowtimeShowtimeId(showtimeId);
        List<ShowtimeSeat> rows = new ArrayList<>();
        for (Seat seat : roomSeats) {
            rows.add(ShowtimeSeat.builder()
                    .showtime(showtime)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }
        showtimeSeatRepository.saveAll(rows);
        log.info("Đã tạo {} showtime_seats cho suất {} (phòng {})",
                rows.size(), showtimeId, showtime.getRoom().getRoomName());
        return rows.size();
    }
}
