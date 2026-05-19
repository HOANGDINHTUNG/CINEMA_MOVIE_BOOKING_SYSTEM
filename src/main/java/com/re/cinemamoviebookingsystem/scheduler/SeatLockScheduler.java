package com.re.cinemamoviebookingsystem.scheduler;

import com.re.cinemamoviebookingsystem.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedRate = 60000)
    public void releaseExpiredLocks() {
        log.debug("Running seat lock cleanup");
        bookingService.releaseExpiredLocks();
    }
}
