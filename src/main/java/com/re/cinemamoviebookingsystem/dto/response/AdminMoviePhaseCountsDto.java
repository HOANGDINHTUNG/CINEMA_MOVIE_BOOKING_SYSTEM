package com.re.cinemamoviebookingsystem.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminMoviePhaseCountsDto {
    /** Đã có lịch chiếu (suất sắp tới). */
    private long hasSchedule;
    /** Đang đợi admin xếp lịch (chưa có suất). */
    private long waitingSchedule;
    /** Hết chiếu tại rạp. */
    private long ended;
    /** Đã ẩn. */
    private long inactive;
    private long total;
}
