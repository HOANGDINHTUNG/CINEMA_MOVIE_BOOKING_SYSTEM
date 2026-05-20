package com.re.cinemamoviebookingsystem.enums;

public enum BookingStatus {
    /** Đang giữ ghế / đang trong luồng thanh toán (tối đa seatLockMinutes). */
    HELD,
    PENDING,
    PAID,
    CANCELLED
}
