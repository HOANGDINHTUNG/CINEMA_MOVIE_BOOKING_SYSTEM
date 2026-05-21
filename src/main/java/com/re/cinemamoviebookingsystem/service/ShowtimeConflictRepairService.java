package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Showtime;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sửa dữ liệu suất chiếu trùng phòng / trùng khung giờ (thường do seed cũ).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeConflictRepairService {

    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public int countOverlappingPairs() {
        return findOverlappingShowtimeIds().size();
    }

    @Transactional(rollbackFor = Exception.class)
    public int cancelOverlappingDuplicates() {
        List<Long> toCancel = findOverlappingShowtimeIds();
        int cancelled = 0;
        for (Long showtimeId : toCancel) {
            Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
            if (showtime == null || showtime.getStatus() == ShowtimeStatus.CANCELLED) {
                continue;
            }
            long paid = bookingRepository.countByShowtimeShowtimeIdAndStatus(showtimeId, BookingStatus.PAID);
            if (paid > 0) {
                log.warn("Skip cancel overlapping showtime {} — has {} paid bookings", showtimeId, paid);
                continue;
            }
            showtime.setStatus(ShowtimeStatus.CANCELLED);
            showtimeRepository.save(showtime);
            cancelled++;
        }
        if (cancelled > 0) {
            log.info("Cancelled {} duplicate/overlapping showtimes", cancelled);
        }
        return cancelled;
    }

    /** Trong từng phòng: giữ suất sớm hơn, hủy suất sau nếu trùng khung [start, end]. */
    private List<Long> findOverlappingShowtimeIds() {
        List<Showtime> active = showtimeRepository.findAll().stream()
                .filter(s -> s.getStatus() != ShowtimeStatus.CANCELLED)
                .filter(s -> s.getRoom() != null && s.getStartTime() != null && s.getEndTime() != null)
                .sorted(Comparator
                        .comparing((Showtime s) -> s.getRoom().getRoomId())
                        .thenComparing(Showtime::getStartTime)
                        .thenComparing(Showtime::getShowtimeId))
                .toList();

        List<Long> cancelIds = new ArrayList<>();
        List<Showtime> keptInRoom = new ArrayList<>();
        Integer currentRoom = null;
        for (Showtime candidate : active) {
            int roomId = candidate.getRoom().getRoomId();
            if (currentRoom == null || currentRoom != roomId) {
                currentRoom = roomId;
                keptInRoom = new ArrayList<>();
            }
            boolean conflicts = keptInRoom.stream().anyMatch(k -> overlaps(k, candidate));
            if (conflicts) {
                cancelIds.add(candidate.getShowtimeId());
            } else {
                keptInRoom.add(candidate);
            }
        }
        return cancelIds;
    }

    private static boolean overlaps(Showtime a, Showtime b) {
        return a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
    }
}
