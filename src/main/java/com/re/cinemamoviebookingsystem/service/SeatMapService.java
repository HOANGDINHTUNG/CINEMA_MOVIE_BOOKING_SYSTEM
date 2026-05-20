package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.response.SeatMapDto;
import com.re.cinemamoviebookingsystem.entity.Showtime;
import com.re.cinemamoviebookingsystem.entity.ShowtimeSeat;
import com.re.cinemamoviebookingsystem.enums.SeatStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.ShowtimeRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatMapService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeSeatRepairService showtimeSeatRepairService;
    private final ShowtimeStatusService showtimeStatusService;
    private final MovieDisplayService movieDisplayService;
    private final CinemaProperties cinemaProperties;

    @Transactional(readOnly = true)
    public SeatMapDto getSeatMap(Long showtimeId) {
        return getSeatMap(showtimeId, null);
    }

    @Transactional(readOnly = true)
    public SeatMapDto getSeatMap(Long showtimeId, Long currentUserId) {
        Showtime showtime = showtimeRepository.findByIdWithDetails(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND, "Suất chiếu không tồn tại"));

        showtimeStatusService.refreshShowtimeStatus(showtimeId);
        showtime = showtimeRepository.findByIdWithDetails(showtimeId).orElseThrow();

        showtimeSeatRepairService.repairShowtimeIfMissing(showtimeId);
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeIdWithSeats(showtimeId);
        if (seats.isEmpty()) {
            int rebuilt = showtimeSeatRepairService.rebuildShowtimeSeats(showtimeId);
            if (rebuilt > 0) {
                seats = showtimeSeatRepository.findByShowtimeIdWithSeats(showtimeId);
            }
            if (seats.isEmpty()) {
                log.warn("Showtime {} vẫn không có ghế sau repair/rebuild", showtimeId);
            }
        }

        LocalDateTime lockExpiresAt = null;
        if (currentUserId != null) {
            lockExpiresAt = seats.stream()
                    .filter(s -> s.getStatus() == SeatStatus.LOCKED
                            && s.getLockedByUser() != null
                            && currentUserId.equals(s.getLockedByUser().getUserId())
                            && s.getLockedUntil() != null
                            && s.getLockedUntil().isAfter(LocalDateTime.now()))
                    .map(ShowtimeSeat::getLockedUntil)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
        }

        return SeatMapDto.builder()
                .showtimeId(showtimeId)
                .movieId(showtime.getMovie().getMovieId())
                .tmdbId(showtime.getMovie().getTmdbId())
                .movieTitle(movieDisplayService.resolveTitleLocal(showtime.getMovie()))
                .roomName(showtime.getRoom().getRoomName())
                .startTime(showtime.getStartTime())
                .basePrice(showtime.getBasePrice())
                .showtimeStatus(showtime.getStatus())
                .soldOut(showtime.getStatus() == ShowtimeStatus.SOLD_OUT)
                .lockExpiresAt(lockExpiresAt)
                .seats(seats.stream().map(ss -> {
                    boolean lockedByMe = currentUserId != null
                            && ss.getStatus() == SeatStatus.LOCKED
                            && !isLockExpired(ss)
                            && ss.getLockedByUser() != null
                            && currentUserId.equals(ss.getLockedByUser().getUserId());
                    return SeatMapDto.SeatCellDto.builder()
                            .seatId(ss.getSeat().getSeatId())
                            .label(ss.getSeat().getLabel())
                            .seatType(ss.getSeat().getSeatType().name())
                            .status(displayStatusForUser(ss, currentUserId))
                            .lockedByCurrentUser(lockedByMe)
                            .build();
                }).collect(Collectors.toList()))
                .build();
    }

    public BigDecimal estimateSeatTotal(SeatMapDto seatMap, List<Long> seatIds) {
        if (seatMap == null || seatIds == null || seatIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return seatMap.getSeats().stream()
                .filter(s -> seatIds.contains(s.getSeatId()))
                .map(s -> {
                    if ("VIP".equalsIgnoreCase(s.getSeatType())) {
                        return seatMap.getBasePrice()
                                .multiply(BigDecimal.valueOf(cinemaProperties.getVipPriceMultiplier()))
                                .setScale(0, RoundingMode.HALF_UP);
                    }
                    return seatMap.getBasePrice();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SeatStatus displayStatusForUser(ShowtimeSeat ss, Long currentUserId) {
        if (ss.getStatus() == SeatStatus.LOCKED) {
            if (isLockExpired(ss)) {
                return SeatStatus.AVAILABLE;
            }
            if (currentUserId != null
                    && ss.getLockedByUser() != null
                    && currentUserId.equals(ss.getLockedByUser().getUserId())) {
                return SeatStatus.AVAILABLE;
            }
        }
        return ss.getStatus();
    }

    private boolean isLockExpired(ShowtimeSeat ss) {
        return ss.getLockedUntil() != null && ss.getLockedUntil().isBefore(LocalDateTime.now());
    }
}
