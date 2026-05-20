package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingHeldStatusTest {

    @Test
    void heldStatusExistsForCheckoutInProgress() {
        assertThat(BookingStatus.valueOf("HELD")).isEqualTo(BookingStatus.HELD);
        assertThat(BookingStatus.values()).contains(BookingStatus.HELD);
    }
}
