package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.response.BookingHistoryDto;
import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.entity.ShowtimeSeat;
import com.re.cinemamoviebookingsystem.entity.Ticket;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeSeatRepository;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingHistoryService {

    private final BookingRepository bookingRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final CinemaProperties cinemaProperties;
    private final MovieDisplayService movieDisplayService;

    @Transactional(readOnly = true)
    public List<BookingHistoryDto> getHistoryForUser(Long userId) {
        return bookingRepository.findByUserWithDetails(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingHistoryDto getBookingDetail(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Đơn không tồn tại"));
        if (!booking.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Không có quyền xem đơn này");
        }
        return toDto(booking);
    }

    private BookingHistoryDto toDto(Booking booking) {
        Long userId = booking.getUser().getUserId();
        Long showtimeId = booking.getShowtime().getShowtimeId();
        LocalDateTime now = LocalDateTime.now();

        List<String> seatLabels;
        List<String> ticketCodes;
        LocalDateTime lockExpiresAt = null;
        boolean heldActive = false;

        if (booking.getStatus() == BookingStatus.HELD) {
            List<ShowtimeSeat> activeLocks = showtimeSeatRepository.findActiveLocksByUserAndShowtime(
                    userId, showtimeId, now);
            seatLabels = activeLocks.stream()
                    .map(ss -> ss.getSeat().getLabel())
                    .sorted()
                    .collect(Collectors.toList());
            lockExpiresAt = activeLocks.stream()
                    .map(ShowtimeSeat::getLockedUntil)
                    .filter(java.util.Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            heldActive = !activeLocks.isEmpty();
            ticketCodes = List.of();
        } else {
            seatLabels = booking.getTickets().stream()
                    .map(t -> t.getSeat().getLabel())
                    .sorted()
                    .collect(Collectors.toList());
            ticketCodes = booking.getTickets().stream()
                    .map(Ticket::getTicketCode)
                    .sorted()
                    .collect(Collectors.toList());
        }

        boolean cancellable = booking.getStatus() == BookingStatus.PAID
                && Duration.between(LocalDateTime.now(), booking.getShowtime().getStartTime()).toHours()
                >= cinemaProperties.getCancelHoursBefore();

        String voucherCode = null;
        if (booking.getUserVoucher() != null && booking.getUserVoucher().getVoucher() != null) {
            voucherCode = booking.getUserVoucher().getVoucher().getCode();
        }

        return BookingHistoryDto.builder()
                .bookingId(booking.getBookingId())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .subtotalAmount(booking.getSubtotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .totalAmount(booking.getTotalAmount())
                .voucherCode(voucherCode)
                .movieTitle(movieDisplayService.resolveTitle(booking.getShowtime().getMovie(), AppLanguage.VI_VN))
                .showtimeStart(booking.getShowtime().getStartTime())
                .roomName(booking.getShowtime().getRoom().getRoomName())
                .seatLabels(seatLabels)
                .ticketCodes(ticketCodes)
                .cancellable(cancellable)
                .showtimeId(showtimeId)
                .lockExpiresAt(lockExpiresAt)
                .heldActive(heldActive)
                .build();
    }
}
