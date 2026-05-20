package com.re.cinemamoviebookingsystem.enums;

/**
 * Giai đoạn phim <strong>trong rạp</strong> (theo lịch chiếu DB), không phải TMDB Now Playing / Upcoming.
 */
public enum AdminMovieScreeningPhase {
    /** ACTIVE, đã có ít nhất một suất chiếu sắp tới. */
    HAS_SCHEDULE("Đã có lịch chiếu", "fa-calendar-check", "phase-scheduled"),
    /** ACTIVE, đã đăng rạp nhưng chưa tạo suất — đang chờ admin xếp lịch. */
    WAITING_SCHEDULE("Đang đợi lịch chiếu", "fa-hourglass-half", "phase-waiting"),
    /** ACTIVE, từng có suất nhưng không còn suất tương lai. */
    ENDED("Hết chiếu tại rạp", "fa-flag-checkered", "phase-ended"),
    /** Đã ẩn khỏi rạp. */
    INACTIVE("Đã ẩn", "fa-eye-slash", "phase-hidden");

    private final String labelVi;
    private final String iconClass;
    private final String cssClass;

    AdminMovieScreeningPhase(String labelVi, String iconClass, String cssClass) {
        this.labelVi = labelVi;
        this.iconClass = iconClass;
        this.cssClass = cssClass;
    }

    public String getLabelVi() {
        return labelVi;
    }

    public String getIconClass() {
        return iconClass;
    }

    public String getCssClass() {
        return cssClass;
    }
}
