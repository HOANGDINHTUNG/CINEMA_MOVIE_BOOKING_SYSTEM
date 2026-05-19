package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.StaffBookingDetailDto;
import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.entity.Ticket;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.repository.TicketRepository;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffBookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final BookingService bookingService;
    private final MovieDisplayService movieDisplayService;

    @Transactional(readOnly = true)
    public StaffBookingDetailDto findByBookingId(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy đơn"));
        return toDto(booking);
    }

    @Transactional(readOnly = true)
    public StaffBookingDetailDto findByTicketCode(String code) {
        Ticket ticket = ticketRepository.findByTicketCodeWithDetails(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND, "Không tìm thấy vé"));
        return toDto(ticket.getBooking());
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmAtCounter(Long bookingId) {
        bookingService.confirmPaymentAtCounter(bookingId);
    }

    private StaffBookingDetailDto toDto(Booking booking) {
        String customerName = booking.getUser().getProfile() != null
                ? booking.getUser().getProfile().getFullName() : booking.getUser().getUsername();
        String phone = booking.getUser().getProfile() != null
                ? booking.getUser().getProfile().getPhoneNumber() : "";

        List<String> seatLabels = booking.getTickets().stream()
                .map(t -> t.getSeat().getLabel())
                .sorted()
                .collect(Collectors.toList());
        List<String> codes = booking.getTickets().stream()
                .map(Ticket::getTicketCode)
                .sorted()
                .collect(Collectors.toList());

        return StaffBookingDetailDto.builder()
                .bookingId(booking.getBookingId())
                .customerName(customerName)
                .customerPhone(phone)
                .movieTitle(movieDisplayService.resolveTitle(booking.getShowtime().getMovie(), AppLanguage.VI_VN))
                .showtimeStart(booking.getShowtime().getStartTime())
                .roomName(booking.getShowtime().getRoom().getRoomName())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .seatLabels(seatLabels)
                .ticketCodes(codes)
                .build();
    }
}
