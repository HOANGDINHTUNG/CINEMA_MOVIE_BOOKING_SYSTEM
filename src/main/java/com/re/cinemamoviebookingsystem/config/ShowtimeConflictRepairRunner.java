package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.service.ShowtimeConflictRepairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(110)
@RequiredArgsConstructor
@Slf4j
public class ShowtimeConflictRepairRunner implements ApplicationRunner {

    private final CinemaProperties cinemaProperties;
    private final ShowtimeConflictRepairService showtimeConflictRepairService;

    @Override
    public void run(ApplicationArguments args) {
        if (!cinemaProperties.isRepairShowtimeConflictsOnStartup()) {
            return;
        }
        int pairs = showtimeConflictRepairService.countOverlappingPairs();
        if (pairs == 0) {
            return;
        }
        log.warn("Phát hiện {} suất chiếu trùng phòng/khung giờ — đang hủy bản trùng (giữ suất ID nhỏ hơn)", pairs);
        int cancelled = showtimeConflictRepairService.cancelOverlappingDuplicates();
        log.info("Đã hủy {} suất trùng lịch", cancelled);
    }
}
