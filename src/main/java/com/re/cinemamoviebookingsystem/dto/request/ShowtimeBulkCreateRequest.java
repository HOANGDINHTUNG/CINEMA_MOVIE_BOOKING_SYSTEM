package com.re.cinemamoviebookingsystem.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ShowtimeBulkCreateRequest {

    private Long movieId;

    private Long tmdbId;

    @NotNull(message = "Chọn phòng chiếu")
    private Integer roomId;

    @NotNull(message = "Chọn Từ ngày")
    private LocalDate startDate;

    @NotNull(message = "Chọn Đến ngày")
    private LocalDate endDate;

    @NotNull(message = "Nhập giá cơ bản")
    @DecimalMin(value = "0", message = "Giá phải ≥ 0")
    private BigDecimal basePrice;

    /** Khung giờ: 10:00,14:00,18:00 */
    private String timeSlots;

    @AssertTrue(message = "Chọn phim trước khi tạo suất")
    public boolean isMovieSelected() {
        return movieId != null || tmdbId != null;
    }

    @AssertTrue(message = "Từ ngày không được trước hôm nay")
    public boolean isStartDateNotPast() {
        if (startDate == null) {
            return true;
        }
        return !startDate.isBefore(LocalDate.now());
    }

    @AssertTrue(message = "Đến ngày không được trước hôm nay")
    public boolean isEndDateNotPast() {
        if (endDate == null) {
            return true;
        }
        return !endDate.isBefore(LocalDate.now());
    }

    @AssertTrue(message = "Đến ngày phải sau hoặc bằng Từ ngày")
    public boolean isEndOnOrAfterStart() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}
