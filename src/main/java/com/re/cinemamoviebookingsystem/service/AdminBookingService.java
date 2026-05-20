package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.AdminBookingSummaryDto;
import com.re.cinemamoviebookingsystem.dto.response.StaffBookingDetailDto;
import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final StaffBookingService staffBookingService;
    private final BookingService bookingService;
    private final MovieDisplayService movieDisplayService;

    @Transactional(readOnly = true)
    public Page<AdminBookingSummaryDto> list(BookingStatus status, Long bookingId, Pageable pageable) {
        return bookingRepository.findForAdmin(status, bookingId, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public StaffBookingDetailDto detail(Long bookingId) {
        return staffBookingService.findByBookingId(bookingId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmAtCounter(Long bookingId) {
        staffBookingService.confirmAtCounter(bookingId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPending(Long bookingId) {
        bookingService.cancelPendingByAdmin(bookingId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPaid(Long bookingId, boolean force) {
        bookingService.cancelPaidByAdmin(bookingId, force);
    }

    private AdminBookingSummaryDto toSummary(Booking booking) {
        String customerName = booking.getUser().getProfile() != null
                ? booking.getUser().getProfile().getFullName()
                : booking.getUser().getUsername();
        return AdminBookingSummaryDto.builder()
                .bookingId(booking.getBookingId())
                .customerName(customerName)
                .movieTitle(movieDisplayService.resolveTitle(
                        booking.getShowtime().getMovie(), AppLanguage.VI_VN))
                .showtimeStart(booking.getShowtime().getStartTime())
                .roomName(booking.getShowtime().getRoom().getRoomName())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .bookingDate(booking.getBookingDate())
                .build();
    }
}
