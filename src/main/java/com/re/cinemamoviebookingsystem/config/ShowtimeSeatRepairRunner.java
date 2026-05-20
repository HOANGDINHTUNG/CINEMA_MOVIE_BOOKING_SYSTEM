package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.repository.RoomRepository;
import com.re.cinemamoviebookingsystem.service.ShowtimeSeatRepairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sau khi nạp seed: bổ sung {@code showtime_seats} cho suất thiếu ghế.
 */
@Component
@Order(105)
@RequiredArgsConstructor
@Slf4j
public class ShowtimeSeatRepairRunner implements ApplicationRunner {

    private final RoomRepository roomRepository;
    private final ShowtimeSeatRepairService showtimeSeatRepairService;

    @Override
    public void run(ApplicationArguments args) {
        if (roomRepository.count() == 0) {
            return;
        }
        int added = showtimeSeatRepairService.repairAllUpcomingMissingSeats();
        if (added == 0) {
            log.debug("Showtime seat repair: không có suất thiếu ghế");
        }
    }
}
