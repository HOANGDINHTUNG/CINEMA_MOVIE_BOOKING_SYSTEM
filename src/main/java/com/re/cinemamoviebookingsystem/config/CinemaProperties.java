package com.re.cinemamoviebookingsystem.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cinema")
public class CinemaProperties {
    private int seatLockMinutes = 15;
    private int cancelHoursBefore = 24;
    private int cleaningBufferMinutes = 15;
    private double vipPriceMultiplier = 1.5;
    /** Số ghế tối đa mỗi lần đặt (một đơn). */
    private int maxSeatsPerBooking = 8;

    /**
     * Khi không có suất chiếu sắp tới: tự đăng vài phim TMDB demo (cần API key + phòng trong DB).
     */
    private boolean demoSeedOnStartup = true;

    /** Số phim tối thiểu «đang chiếu» (có suất) khi demo seed chạy. */
    private int demoSeedNowShowingTarget = 40;

    /** Số phim tối thiểu «sắp chiếu» (đăng rạp, chưa có suất). */
    private int demoSeedComingSoonTarget = 18;
}
