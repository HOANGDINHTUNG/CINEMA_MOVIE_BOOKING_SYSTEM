package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.entity.Ticket;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final BookingRepository bookingRepository;

    @Async
    public void sendTicketEmailAsync(Long bookingId) {
        bookingRepository.findByIdWithDetails(bookingId).ifPresent(booking -> {
            String email = booking.getUser().getEmail();
            String codes = booking.getTickets().stream()
                    .map(Ticket::getTicketCode)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            log.info("[ASYNC EMAIL] Gửi mã vé QR tới {} cho booking #{}: {}", email, bookingId, codes);
        });
    }
}
