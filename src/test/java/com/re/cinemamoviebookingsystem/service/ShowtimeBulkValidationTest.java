package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.ShowtimeBulkCreateRequest;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ShowtimeBulkValidationTest {

    @InjectMocks
    private ShowtimeService showtimeService;

    @Mock
    private com.re.cinemamoviebookingsystem.repository.ShowtimeRepository showtimeRepository;

    @Test
    void filterRejectsToBeforeFrom() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                showtimeService.validateAdminFilterDateRange(
                        LocalDate.of(2026, 6, 10),
                        LocalDate.of(2026, 6, 1)));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void filterAllowsValidRange() {
        assertDoesNotThrow(() ->
                showtimeService.validateAdminFilterDateRange(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 10)));
    }
}
