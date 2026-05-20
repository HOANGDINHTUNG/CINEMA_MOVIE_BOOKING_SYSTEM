package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.request.ChangePasswordRequest;
import com.re.cinemamoviebookingsystem.dto.response.AccountOverviewDto;
import com.re.cinemamoviebookingsystem.entity.User;
import com.re.cinemamoviebookingsystem.enums.BookingStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.BookingRepository;
import com.re.cinemamoviebookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AccountOverviewDto getOverview(Long userId) {
        var bookings = bookingRepository.findByUserWithDetails(userId);
        LocalDateTime now = LocalDateTime.now();
        long paid = 0;
        long pending = 0;
        long held = 0;
        long cancelled = 0;
        long upcomingPaid = 0;
        for (var b : bookings) {
            if (b.getStatus() == BookingStatus.PAID) {
                paid++;
                if (b.getShowtime().getStartTime().isAfter(now)) {
                    upcomingPaid++;
                }
            } else if (b.getStatus() == BookingStatus.PENDING) {
                pending++;
            } else if (b.getStatus() == BookingStatus.HELD) {
                held++;
            } else if (b.getStatus() == BookingStatus.CANCELLED) {
                cancelled++;
            }
        }
        return AccountOverviewDto.builder()
                .totalBookings(bookings.size())
                .paidCount(paid)
                .pendingCount(pending)
                .heldCount(held)
                .cancelledCount(cancelled)
                .upcomingPaidCount(upcomingPaid)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Mật khẩu xác nhận không khớp");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "User không tồn tại"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
