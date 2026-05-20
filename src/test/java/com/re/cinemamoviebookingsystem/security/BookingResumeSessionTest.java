package com.re.cinemamoviebookingsystem.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingResumeSessionTest {

    @Test
    void saveAndReadPendingBooking() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        BookingResumeSession.save(request, 42L, List.of(1L, 2L));

        assertThat(BookingResumeSession.getShowtimeId(request)).isEqualTo(42L);
        assertThat(BookingResumeSession.getSeatIds(request)).containsExactly(1L, 2L);

        BookingResumeSession.clear(request);
        assertThat(BookingResumeSession.getShowtimeId(request)).isNull();
        assertThat(BookingResumeSession.getSeatIds(request)).isNull();
    }
}
