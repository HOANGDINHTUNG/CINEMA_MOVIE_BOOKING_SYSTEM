package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.entity.Payment;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.enums.PaymentStatus;
import com.re.cinemamoviebookingsystem.enums.SeatStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.repository.PaymentRepository;
import com.re.cinemamoviebookingsystem.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VnPaySandboxService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeStatusService showtimeStatusService;
    private final EmailNotificationService emailNotificationService;

    @Value("${cinema.vnpay.return-url:http://localhost:8080/payment/vnpay-return}")
    private String returnUrl;

    @Transactional(rollbackFor = Exception.class)
    public String createPaymentUrl(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Đơn không tồn tại"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATUS, "Đơn không chờ thanh toán online");
        }
        String txnId = "VNPAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = booking.getPayment();
        if (payment == null) {
            payment = Payment.builder().booking(booking).amount(booking.getTotalAmount()).build();
            booking.setPayment(payment);
        }
        payment.setPaymentMethod("VNPAY");
        payment.setTransactionId(txnId);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        return returnUrl + "?bookingId=" + bookingId + "&txnId=" + txnId + "&status=SUCCESS";
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleReturn(Long bookingId, String txnId, String status) {
        if (!"SUCCESS".equalsIgnoreCase(status)) {
            return;
        }
        Payment payment = paymentRepository.findByTransactionId(txnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Giao dịch không hợp lệ"));
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return;
        }
        Booking booking = payment.getBooking();
        if (!booking.getBookingId().equals(bookingId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Booking không khớp");
        }

        List<Long> seatIds = booking.getTickets().stream().map(t -> t.getSeat().getSeatId()).toList();
        var seats = showtimeSeatRepository.findByShowtimeAndSeatIdsForUpdate(
                booking.getShowtime().getShowtimeId(), seatIds);
        for (var ss : seats) {
            ss.setStatus(SeatStatus.BOOKED);
            ss.setLockedByUser(null);
            ss.setLockedUntil(null);
        }
        showtimeSeatRepository.saveAll(seats);

        booking.setStatus(BookingStatus.PAID);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        bookingRepository.save(booking);
        paymentRepository.save(payment);
        showtimeStatusService.refreshShowtimeStatus(booking.getShowtime().getShowtimeId());
        emailNotificationService.sendTicketEmailAsync(bookingId);
    }
}
