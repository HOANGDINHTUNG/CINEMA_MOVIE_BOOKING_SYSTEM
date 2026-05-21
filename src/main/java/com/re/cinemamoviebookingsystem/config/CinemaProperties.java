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

    private String brandName = "Smart Cinema";

    private int seatLockMinutes = 15;
    private int cancelHoursBefore = 24;
    private int cleaningBufferMinutes = 15;
    private double vipPriceMultiplier = 1.5;
    private int maxSeatsPerBooking = 8;

    private boolean demoSeedOnStartup = true;

    /** Khi khởi động: tự hủy suất trùng phòng (dữ liệu seed/demo cũ). */
    private boolean repairShowtimeConflictsOnStartup = true;

    /** TMDB now_playing, đăng không suất → «Đang đợi lịch chiếu». */
    private int demoSeedWaitingTarget = 100;

    /** Phim có lịch mẫu (id tách khỏi waiting). 0 = không auto-đăng. */
    private int demoSeedScheduledTarget = 0;

    /** Cũ: ánh xạ sang waiting nếu waiting=0. */
    private int demoSeedNowShowingTarget = 0;

    /** Không dùng upcoming cho seed đang đợi. */
    private int demoSeedComingSoonTarget = 0;

    private int homeComingSoonMax = 100;

    public int getDemoSeedWaitingTarget() {
        if (demoSeedWaitingTarget > 0) {
            return demoSeedWaitingTarget;
        }
        return demoSeedNowShowingTarget > 0 ? demoSeedNowShowingTarget : demoSeedWaitingTarget;
    }

    public void setDemoSeedWaitingTarget(int value) {
        this.demoSeedWaitingTarget = value;
    }

    /** Cài đặt admin cũ — đồng bộ với waiting target. */
    public int getDemoSeedNowShowingTarget() {
        return getDemoSeedWaitingTarget();
    }

    public void setDemoSeedNowShowingTarget(int value) {
        setDemoSeedWaitingTarget(value);
    }
}
