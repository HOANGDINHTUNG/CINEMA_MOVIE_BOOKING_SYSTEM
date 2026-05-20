package com.re.cinemamoviebookingsystem;

import com.re.cinemamoviebookingsystem.service.MovieDisplayService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MovieDisplayServiceTest {

    @Test
    void titleFromAdminNote_parsesAfterColon() {
        assertEquals("Shrek", MovieDisplayService.titleFromAdminNote("now_playing: Shrek"));
    }

    @Test
    void titleFromAdminNote_returnsNullWhenBlank() {
        assertNull(MovieDisplayService.titleFromAdminNote("   "));
        assertNull(MovieDisplayService.titleFromAdminNote(null));
    }
}
