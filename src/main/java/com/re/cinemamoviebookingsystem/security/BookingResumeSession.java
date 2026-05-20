package com.re.cinemamoviebookingsystem.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public final class BookingResumeSession {

    public static final String SHOWTIME_ID = "BOOKING_PENDING_SHOWTIME_ID";
    public static final String SEAT_IDS = "BOOKING_PENDING_SEAT_IDS";

    private BookingResumeSession() {
    }

    public static void save(HttpServletRequest request, Long showtimeId, List<Long> seatIds) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SHOWTIME_ID, showtimeId);
        session.setAttribute(SEAT_IDS, seatIds);
    }

    @SuppressWarnings("unchecked")
    public static Long getShowtimeId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SHOWTIME_ID);
        return value instanceof Long ? (Long) value : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Long> getSeatIds(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SEAT_IDS);
        if (value instanceof List<?> list) {
            return list.stream().filter(Long.class::isInstance).map(Long.class::cast).toList();
        }
        return null;
    }

    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SHOWTIME_ID);
            session.removeAttribute(SEAT_IDS);
        }
    }
}
