package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.ShowtimeCreateRequest;
import com.re.cinemamoviebookingsystem.entity.Movie;
import com.re.cinemamoviebookingsystem.entity.Room;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.MovieRepository;
import com.re.cinemamoviebookingsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Tạo lịch chiếu ban đầu khi admin đăng phim lên rạp (không gọi từ trang khách).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeScheduleService {

    private static final LocalTime[] DAILY_SLOTS = {
            LocalTime.of(10, 0),
            LocalTime.of(13, 30),
            LocalTime.of(16, 0),
            LocalTime.of(19, 0),
            LocalTime.of(21, 30)
    };
    private static final int SCHEDULE_DAYS = 10;
    private static final BigDecimal[] SLOT_PRICES = {
            new BigDecimal("75000"),
            new BigDecimal("85000"),
            new BigDecimal("90000"),
            new BigDecimal("105000"),
            new BigDecimal("115000")
    };

    private final ShowtimeService showtimeService;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;

    @Transactional(rollbackFor = Exception.class)
    public int generateInitialSchedule(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phim không tồn tại"));
        if (movie.getTmdbId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Phim chưa có tmdbId");
        }
        if (!showtimeService.listByTmdbId(movie.getTmdbId()).isEmpty()) {
            log.info("Movie {} already has showtimes — skip generation", movieId);
            return 0;
        }

        List<Room> rooms = roomRepository.findAll();
        if (rooms.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Chưa cấu hình phòng chiếu");
        }

        long tmdbId = movie.getTmdbId();
        LocalDateTime earliest = LocalDateTime.now().plusHours(1);
        int created = 0;
        int roomOffset = (int) (Math.abs(tmdbId) % rooms.size());

        for (int dayOffset = 0; dayOffset < SCHEDULE_DAYS; dayOffset++) {
            LocalDate day = LocalDate.now().plusDays(dayOffset);
            for (int slotIndex = 0; slotIndex < DAILY_SLOTS.length; slotIndex++) {
                LocalDateTime start = day.atTime(DAILY_SLOTS[slotIndex]);
                if (start.isBefore(earliest)) {
                    continue;
                }
                Room room = rooms.get((roomOffset + slotIndex + dayOffset) % rooms.size());
                ShowtimeCreateRequest request = new ShowtimeCreateRequest();
                request.setTmdbId(tmdbId);
                request.setRoomId(room.getRoomId());
                request.setStartTime(start);
                request.setBasePrice(SLOT_PRICES[slotIndex]);
                try {
                    showtimeService.createShowtime(request);
                    created++;
                } catch (BusinessException ex) {
                    if (ex.getErrorCode() != ErrorCode.ROOM_CONFLICT) {
                        throw ex;
                    }
                }
            }
        }
        log.info("Generated {} showtimes for movieId={} tmdbId={}", created, movieId, tmdbId);
        return created;
    }
}
