package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.request.CheckoutRequest;
import com.re.cinemamoviebookingsystem.entity.*;
import com.re.cinemamoviebookingsystem.enums.*;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final UserRepository userRepository;
    private final ComboRepository comboRepository;
    private final PaymentRepository paymentRepository;
    private final ShowtimeStatusService showtimeStatusService;
    private final CinemaProperties cinemaProperties;
    private final EmailNotificationService emailNotificationService;
    private final VoucherService voucherService;

    @Transactional(rollbackFor = Exception.class)
    public void lockSeats(Long showtimeId, List<Long> seatIds, Long userId) {
        validateSeatCount(seatIds);
        validateShowtimeBookable(showtimeId);
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeAndSeatIdsForUpdate(showtimeId, seatIds);
        if (seats.size() != seatIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_TAKEN, "Một hoặc nhiều ghế không tồn tại");
        }
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(cinemaProperties.getSeatLockMinutes());

        for (ShowtimeSeat ss : seats) {
            if (!canLock(ss, userId)) {
                throw new BusinessException(ErrorCode.SEAT_TAKEN,
                        "Ghế " + ss.getSeat().getLabel() + " đã có người đặt");
            }
            ss.setStatus(SeatStatus.LOCKED);
            ss.setLockedByUser(user);
            ss.setLockedUntil(lockUntil);
        }
        showtimeSeatRepository.saveAll(seats);
        upsertHeldBooking(showtimeId, seatIds, userId);
        showtimeStatusService.refreshShowtimeStatus(showtimeId);
    }

    /**
     * Cập nhật bộ ghế đang giữ sau khi user bỏ chọn / chọn thêm trên trang ghế (đã lock server).
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncSeatLocks(Long showtimeId, List<Long> seatIds, Long userId) {
        validateShowtimeBookable(showtimeId);
        LocalDateTime now = LocalDateTime.now();
        List<Long> targetIds = seatIds != null ? seatIds : List.of();
        if (!targetIds.isEmpty()) {
            validateSeatCount(targetIds);
        }

        List<ShowtimeSeat> currentLocks = showtimeSeatRepository.findActiveLocksByUserAndShowtime(
                userId, showtimeId, now);
        Set<Long> targetSet = new HashSet<>(targetIds);

        List<ShowtimeSeat> toRelease = new ArrayList<>();
        for (ShowtimeSeat ss : currentLocks) {
            if (!targetSet.contains(ss.getSeat().getSeatId())) {
                ss.setStatus(SeatStatus.AVAILABLE);
                ss.setLockedByUser(null);
                ss.setLockedUntil(null);
                toRelease.add(ss);
            }
        }
        if (!toRelease.isEmpty()) {
            showtimeSeatRepository.saveAll(toRelease);
        }

        if (targetIds.isEmpty()) {
            cancelHeldBookingIfNoActiveLocks(userId, showtimeId, now);
            showtimeStatusService.refreshShowtimeStatus(showtimeId);
            return;
        }

        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime lockUntil = now.plusMinutes(cinemaProperties.getSeatLockMinutes());
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeAndSeatIdsForUpdate(showtimeId, targetIds);
        if (seats.size() != targetIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_TAKEN, "Một hoặc nhiều ghế không tồn tại");
        }

        for (ShowtimeSeat ss : seats) {
            if (ss.getStatus() == SeatStatus.LOCKED
                    && ss.getLockedByUser() != null
                    && ss.getLockedByUser().getUserId().equals(userId)
                    && ss.getLockedUntil() != null
                    && ss.getLockedUntil().isAfter(now)) {
                ss.setLockedUntil(lockUntil);
                continue;
            }
            if (!canLock(ss, userId)) {
                throw new BusinessException(ErrorCode.SEAT_TAKEN,
                        "Ghế " + ss.getSeat().getLabel() + " đã có người đặt");
            }
            ss.setStatus(SeatStatus.LOCKED);
            ss.setLockedByUser(user);
            ss.setLockedUntil(lockUntil);
        }
        showtimeSeatRepository.saveAll(seats);
        upsertHeldBooking(showtimeId, targetIds, userId);
        showtimeStatusService.refreshShowtimeStatus(showtimeId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSeatLock(Long showtimeId, Long userId) {
        if (userId == null) {
            return false;
        }
        return showtimeSeatRepository.existsActiveLockByUserAndShowtime(
                userId, showtimeId, LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseAllSeatLocks(Long showtimeId, Long userId) {
        syncSeatLocks(showtimeId, List.of(), userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long checkout(CheckoutRequest request, Long userId) {
        validateSeatCount(request.getSeatIds());
        validateShowtimeBookable(request.getShowtimeId());
        lockSeats(request.getShowtimeId(), request.getSeatIds(), userId);

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId()).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeAndSeatIdsForUpdate(
                request.getShowtimeId(), request.getSeatIds());

        verifySeatsLockedByUser(seats, userId);

        BigDecimal ticketTotal = calculateTicketTotal(showtime, seats);
        BigDecimal comboTotal = calculateComboTotal(request.getComboQuantities());
        BigDecimal subtotal = ticketTotal.add(comboTotal);
        boolean hasCombo = comboTotal.compareTo(BigDecimal.ZERO) > 0;

        BigDecimal discount = BigDecimal.ZERO;
        UserVoucher appliedVoucher = null;
        if (request.getUserVoucherId() != null) {
            appliedVoucher = voucherService.resolveForCheckout(
                    request.getUserVoucherId(), userId, subtotal, hasCombo);
            discount = voucherService.calculateDiscount(appliedVoucher.getVoucher(), subtotal, hasCombo);
        }
        BigDecimal total = subtotal.subtract(discount);

        BookingStatus bookingStatus = request.getPaymentMode() == PaymentMode.ONLINE
                ? BookingStatus.PAID : BookingStatus.PENDING;

        Booking booking = bookingRepository
                .findByUser_UserIdAndShowtime_ShowtimeIdAndStatus(userId, showtime.getShowtimeId(), BookingStatus.HELD)
                .orElseGet(() -> Booking.builder()
                        .user(user)
                        .showtime(showtime)
                        .build());
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setSubtotalAmount(subtotal);
        booking.setDiscountAmount(discount);
        booking.setTotalAmount(total);
        booking.setUserVoucher(appliedVoucher);
        booking.setStatus(bookingStatus);
        booking.setBookingDate(LocalDateTime.now());
        booking.getTickets().clear();
        booking.getBookingCombos().clear();

        if (request.getComboQuantities() != null) {
            for (Map.Entry<Integer, Integer> entry : request.getComboQuantities().entrySet()) {
                if (entry.getValue() == null || entry.getValue() <= 0) continue;
                Combo combo = comboRepository.findById(entry.getKey()).orElseThrow();
                BookingCombo bc = BookingCombo.builder()
                        .id(new BookingComboId(null, combo.getComboId()))
                        .booking(booking)
                        .combo(combo)
                        .quantity(entry.getValue())
                        .price(combo.getPrice())
                        .build();
                booking.getBookingCombos().add(bc);
            }
        }

        for (ShowtimeSeat ss : seats) {
            BigDecimal price = seatPrice(showtime, ss.getSeat());
            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .seat(ss.getSeat())
                    .ticketCode(UUID.randomUUID().toString())
                    .price(price)
                    .build();
            booking.getTickets().add(ticket);

            if (bookingStatus == BookingStatus.PAID) {
                ss.setStatus(SeatStatus.BOOKED);
                ss.setLockedByUser(null);
                ss.setLockedUntil(null);
            }
        }

        booking = bookingRepository.save(booking);
        showtimeSeatRepository.saveAll(seats);

        if (appliedVoucher != null) {
            voucherService.markUsed(appliedVoucher, booking);
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(request.getPaymentMode() == PaymentMode.ONLINE ? "ONLINE" : "CASH")
                .amount(total)
                .paymentStatus(bookingStatus == BookingStatus.PAID ? PaymentStatus.SUCCESS : PaymentStatus.PENDING)
                .paymentDate(bookingStatus == BookingStatus.PAID ? LocalDateTime.now() : null)
                .build();
        paymentRepository.save(payment);

        if (bookingStatus == BookingStatus.PAID) {
            showtimeStatusService.refreshShowtimeStatus(showtime.getShowtimeId());
            emailNotificationService.sendTicketEmailAsync(booking.getBookingId());
        }

        return booking.getBookingId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmPaymentAtCounter(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Đơn không tồn tại"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATUS, "Đơn không ở trạng thái chờ thanh toán");
        }

        List<Long> seatIds = booking.getTickets().stream()
                .map(t -> t.getSeat().getSeatId()).toList();
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeAndSeatIdsForUpdate(
                booking.getShowtime().getShowtimeId(), seatIds);

        for (ShowtimeSeat ss : seats) {
            if (ss.getStatus() != SeatStatus.LOCKED && ss.getStatus() != SeatStatus.AVAILABLE) {
                if (ss.getStatus() == SeatStatus.BOOKED) {
                    throw new BusinessException(ErrorCode.SEAT_TAKEN, "Ghế đã được đặt bởi người khác");
                }
            }
            ss.setStatus(SeatStatus.BOOKED);
            ss.setLockedByUser(null);
            ss.setLockedUntil(null);
        }

        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);
        showtimeSeatRepository.saveAll(seats);

        Payment payment = booking.getPayment();
        if (payment == null) {
            payment = Payment.builder().booking(booking).build();
        }
        payment.setPaymentMethod("CASH");
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setAmount(booking.getTotalAmount());
        paymentRepository.save(payment);

        showtimeStatusService.refreshShowtimeStatus(booking.getShowtime().getShowtimeId());
        emailNotificationService.sendTicketEmailAsync(bookingId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingByAdmin(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Đơn không tồn tại"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATUS, "Chỉ hủy được đơn đang chờ thanh toán");
        }
        releaseSeatsForBooking(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        voucherService.restoreIfUsed(booking);
        bookingRepository.save(booking);
        if (booking.getPayment() != null) {
            booking.getPayment().setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(booking.getPayment());
        }
        showtimeStatusService.refreshShowtimeStatus(booking.getShowtime().getShowtimeId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPaidByAdmin(Long bookingId, boolean skipTimeCheck) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Đơn không tồn tại"));
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATUS, "Chỉ hủy được đơn đã thanh toán");
        }
        if (!skipTimeCheck) {
            LocalDateTime showStart = booking.getShowtime().getStartTime();
            long hoursLeft = Duration.between(LocalDateTime.now(), showStart).toHours();
            if (hoursLeft < cinemaProperties.getCancelHoursBefore()) {
                throw new BusinessException(ErrorCode.CANCEL_TOO_LATE,
                        "Không thể hủy trong vòng " + cinemaProperties.getCancelHoursBefore() + " giờ trước giờ chiếu");
            }
        }
        releaseSeatsForBooking(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        voucherService.restoreIfUsed(booking);
        bookingRepository.save(booking);
        showtimeStatusService.refreshShowtimeStatus(booking.getShowtime().getShowtimeId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Đơn không tồn tại"));
        if (!booking.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Không có quyền hủy đơn này");
        }
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_STATUS, "Chỉ hủy được đơn đã thanh toán");
        }

        LocalDateTime showStart = booking.getShowtime().getStartTime();
        long hoursLeft = Duration.between(LocalDateTime.now(), showStart).toHours();
        if (hoursLeft < cinemaProperties.getCancelHoursBefore()) {
            throw new BusinessException(ErrorCode.CANCEL_TOO_LATE,
                    "Không thể hủy trong vòng " + cinemaProperties.getCancelHoursBefore() + " giờ trước giờ chiếu");
        }

        releaseSeatsForBooking(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        voucherService.restoreIfUsed(booking);
        bookingRepository.save(booking);
        showtimeStatusService.refreshShowtimeStatus(booking.getShowtime().getShowtimeId());
    }

    private void releaseSeatsForBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.HELD) {
            releaseActiveLocksForUserShowtime(
                    booking.getUser().getUserId(), booking.getShowtime().getShowtimeId());
            return;
        }
        List<Long> seatIds = booking.getTickets().stream()
                .map(t -> t.getSeat().getSeatId()).toList();
        if (seatIds.isEmpty()) {
            return;
        }
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeAndSeatIdsForUpdate(
                booking.getShowtime().getShowtimeId(), seatIds);
        for (ShowtimeSeat ss : seats) {
            ss.setStatus(SeatStatus.AVAILABLE);
            ss.setLockedByUser(null);
            ss.setLockedUntil(null);
        }
        showtimeSeatRepository.saveAll(seats);
    }

    private void releaseActiveLocksForUserShowtime(Long userId, Long showtimeId) {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeSeat> locks = showtimeSeatRepository.findActiveLocksByUserAndShowtime(
                userId, showtimeId, now);
        for (ShowtimeSeat ss : locks) {
            ss.setStatus(SeatStatus.AVAILABLE);
            ss.setLockedByUser(null);
            ss.setLockedUntil(null);
        }
        if (!locks.isEmpty()) {
            showtimeSeatRepository.saveAll(locks);
        }
    }

    private void upsertHeldBooking(Long showtimeId, List<Long> seatIds, Long userId) {
        Showtime showtime = showtimeRepository.findById(showtimeId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeAndSeatIdsForUpdate(showtimeId, seatIds);
        BigDecimal total = calculateTicketTotal(showtime, seats);

        Booking booking = bookingRepository
                .findByUser_UserIdAndShowtime_ShowtimeIdAndStatus(userId, showtimeId, BookingStatus.HELD)
                .orElseGet(() -> Booking.builder()
                        .user(user)
                        .showtime(showtime)
                        .status(BookingStatus.HELD)
                        .build());
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setTotalAmount(total);
        booking.setStatus(BookingStatus.HELD);
        booking.setBookingDate(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeSeat> expired = showtimeSeatRepository.findExpiredLocks(now);
        Set<Long> showtimeIds = new HashSet<>();
        Set<String> heldKeys = new HashSet<>();
        for (ShowtimeSeat ss : expired) {
            if (ss.getLockedByUser() != null && ss.getShowtime() != null) {
                heldKeys.add(ss.getLockedByUser().getUserId() + ":" + ss.getShowtime().getShowtimeId());
            }
            ss.setStatus(SeatStatus.AVAILABLE);
            ss.setLockedByUser(null);
            ss.setLockedUntil(null);
            showtimeIds.add(ss.getShowtime().getShowtimeId());
        }
        showtimeSeatRepository.saveAll(expired);

        for (String key : heldKeys) {
            String[] parts = key.split(":");
            Long userId = Long.parseLong(parts[0]);
            Long showtimeId = Long.parseLong(parts[1]);
            cancelHeldBookingIfNoActiveLocks(userId, showtimeId, now);
        }
        cancelOrphanedHeldBookings(now);

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(cinemaProperties.getSeatLockMinutes());
        List<Booking> pendingExpired = bookingRepository.findByStatusAndBookingDateBefore(
                BookingStatus.PENDING, cutoff);
        for (Booking b : pendingExpired) {
            b.setStatus(BookingStatus.CANCELLED);
            for (Ticket t : b.getTickets()) {
                showtimeSeatRepository.findByShowtimeShowtimeIdAndSeatSeatId(
                                b.getShowtime().getShowtimeId(), t.getSeat().getSeatId())
                        .ifPresent(ss -> {
                            if (ss.getStatus() == SeatStatus.LOCKED) {
                                ss.setStatus(SeatStatus.AVAILABLE);
                                ss.setLockedByUser(null);
                                ss.setLockedUntil(null);
                                showtimeSeatRepository.save(ss);
                            }
                        });
            }
        }
        bookingRepository.saveAll(pendingExpired);
        showtimeIds.forEach(showtimeStatusService::refreshShowtimeStatus);
    }

    private void cancelHeldBookingIfNoActiveLocks(Long userId, Long showtimeId, LocalDateTime now) {
        if (showtimeSeatRepository.existsActiveLockByUserAndShowtime(userId, showtimeId, now)) {
            return;
        }
        bookingRepository.findByUser_UserIdAndShowtime_ShowtimeIdAndStatus(userId, showtimeId, BookingStatus.HELD)
                .ifPresent(b -> {
                    b.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(b);
                });
    }

    private void cancelOrphanedHeldBookings(LocalDateTime now) {
        for (Booking held : bookingRepository.findByStatus(BookingStatus.HELD)) {
            if (!showtimeSeatRepository.existsActiveLockByUserAndShowtime(
                    held.getUser().getUserId(), held.getShowtime().getShowtimeId(), now)) {
                held.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(held);
            }
        }
    }

    private void validateSeatCount(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Vui lòng chọn ít nhất một ghế");
        }
        int max = cinemaProperties.getMaxSeatsPerBooking();
        if (seatIds.size() > max) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Mỗi đơn chỉ được đặt tối đa " + max + " ghế");
        }
    }

    private void validateShowtimeBookable(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND, "Suất chiếu không tồn tại"));
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SHOWTIME_STARTED, "Suất chiếu đã bắt đầu");
        }
        if (showtime.getStatus() == ShowtimeStatus.HIDDEN || showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND, "Suất chiếu không khả dụng");
        }
    }

    private boolean canLock(ShowtimeSeat ss, Long userId) {
        if (ss.getStatus() == SeatStatus.BOOKED) return false;
        if (ss.getStatus() == SeatStatus.AVAILABLE) return true;
        if (ss.getStatus() == SeatStatus.LOCKED) {
            if (ss.getLockedUntil() != null && ss.getLockedUntil().isBefore(LocalDateTime.now())) {
                return true;
            }
            return ss.getLockedByUser() != null && ss.getLockedByUser().getUserId().equals(userId);
        }
        return false;
    }

    private void verifySeatsLockedByUser(List<ShowtimeSeat> seats, Long userId) {
        for (ShowtimeSeat ss : seats) {
            if (ss.getStatus() == SeatStatus.BOOKED) {
                throw new BusinessException(ErrorCode.SEAT_TAKEN, "Ghế " + ss.getSeat().getLabel() + " đã có chủ");
            }
            if (ss.getStatus() != SeatStatus.LOCKED
                    || ss.getLockedByUser() == null
                    || !ss.getLockedByUser().getUserId().equals(userId)
                    || (ss.getLockedUntil() != null && ss.getLockedUntil().isBefore(LocalDateTime.now()))) {
                throw new BusinessException(ErrorCode.SEAT_TAKEN, "Ghế " + ss.getSeat().getLabel() + " chưa được giữ");
            }
        }
    }

    private BigDecimal calculateTicketTotal(Showtime showtime, List<ShowtimeSeat> seats) {
        BigDecimal total = BigDecimal.ZERO;
        for (ShowtimeSeat ss : seats) {
            total = total.add(seatPrice(showtime, ss.getSeat()));
        }
        return total;
    }

    private BigDecimal seatPrice(Showtime showtime, Seat seat) {
        if (seat.getSeatType() == PhysicalSeatType.VIP) {
            return showtime.getBasePrice().multiply(
                    BigDecimal.valueOf(cinemaProperties.getVipPriceMultiplier())).setScale(2, RoundingMode.HALF_UP);
        }
        return showtime.getBasePrice();
    }

    private BigDecimal calculateComboTotal(Map<Integer, Integer> comboQuantities) {
        if (comboQuantities == null || comboQuantities.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> e : comboQuantities.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            Combo combo = comboRepository.findById(e.getKey()).orElseThrow();
            total = total.add(combo.getPrice().multiply(BigDecimal.valueOf(e.getValue())));
        }
        return total;
    }
}
