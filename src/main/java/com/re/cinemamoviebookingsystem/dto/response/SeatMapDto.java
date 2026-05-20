package com.re.cinemamoviebookingsystem.dto.response;

import com.re.cinemamoviebookingsystem.enums.SeatStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SeatMapDto {
    private Long showtimeId;
    private Long movieId;
    private Long tmdbId;
    private String movieTitle;
    private String roomName;
    private LocalDateTime startTime;
    private BigDecimal basePrice;
    private ShowtimeStatus showtimeStatus;
    private boolean soldOut;
    /** Thời điểm hết hạn giữ ghế của user hiện tại (nếu đã lock). */
    private LocalDateTime lockExpiresAt;
    private List<SeatCellDto> seats;

    @Getter
    @Builder
    public static class SeatCellDto {
        private Long seatId;
        private String label;
        private String seatType;
        private SeatStatus status;
        /** Ghế đang được user hiện tại giữ (LOCKED). */
        private boolean lockedByCurrentUser;
    }
}
