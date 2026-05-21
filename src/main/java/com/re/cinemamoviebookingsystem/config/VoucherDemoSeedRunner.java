package com.re.cinemamoviebookingsystem.config;

import com.re.cinemamoviebookingsystem.entity.User;
import com.re.cinemamoviebookingsystem.entity.UserVoucher;
import com.re.cinemamoviebookingsystem.entity.Voucher;
import com.re.cinemamoviebookingsystem.enums.DiscountType;
import com.re.cinemamoviebookingsystem.enums.UserVoucherStatus;
import com.re.cinemamoviebookingsystem.enums.VoucherStatus;
import com.re.cinemamoviebookingsystem.repository.UserRepository;
import com.re.cinemamoviebookingsystem.repository.UserVoucherRepository;
import com.re.cinemamoviebookingsystem.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(50)
@RequiredArgsConstructor
@Slf4j
public class VoucherDemoSeedRunner implements ApplicationRunner {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (voucherRepository.count() > 0) {
            return;
        }
        log.info("Seeding demo vouchers...");
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime until = LocalDateTime.of(2027, 12, 31, 23, 59);

        Voucher ncc10 = saveVoucher("NCC10", "Giảm 10% vé phim", "Áp dụng mọi suất 2D",
                DiscountType.PERCENT, BigDecimal.valueOf(10), BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(30_000), from, until, false);
        Voucher ncc50k = saveVoucher("NCC50K", "Giảm 50.000đ", "Đơn từ 200.000đ",
                DiscountType.FIXED, BigDecimal.valueOf(50_000), BigDecimal.valueOf(200_000),
                null, from, until, false);
        Voucher u22 = saveVoucher("U22-55K", "Gen Z -55K", "Ưu đãi khách trẻ",
                DiscountType.FIXED, BigDecimal.valueOf(55_000), BigDecimal.valueOf(150_000),
                null, from, until, false);
        Voucher combo20 = saveVoucher("COMBO20", "Combo -20%", "Cần có combo trong đơn",
                DiscountType.PERCENT, BigDecimal.valueOf(20), BigDecimal.ZERO,
                BigDecimal.valueOf(50_000), from, until, true);
        Voucher welcome = saveVoucher("WELCOME5", "Chào mừng -5%", "Voucher đăng ký mới",
                DiscountType.PERCENT, BigDecimal.valueOf(5), BigDecimal.valueOf(50_000),
                BigDecimal.valueOf(15_000), from, until, false);

        List<Long> customerIds = List.of(3L, 4L, 5L, 6L, 7L, 8L);
        for (Long userId : customerIds) {
            userRepository.findById(userId).ifPresent(user -> {
                assign(user, ncc10);
                assign(user, ncc50k);
                if (userId == 3L || userId == 7L) {
                    assign(user, u22);
                }
                if (userId == 3L) {
                    assign(user, combo20);
                    assign(user, welcome);
                }
                if (userId == 5L) {
                    assign(user, welcome);
                }
            });
        }
        log.info("Demo vouchers seeded for {} customers", customerIds.size());
    }

    private Voucher saveVoucher(String code, String title, String desc, DiscountType type,
                                BigDecimal value, BigDecimal minOrder, BigDecimal maxDiscount,
                                LocalDateTime from, LocalDateTime until, boolean requireCombo) {
        return voucherRepository.save(Voucher.builder()
                .code(code)
                .title(title)
                .description(desc)
                .discountType(type)
                .discountValue(value)
                .minOrderAmount(minOrder)
                .maxDiscountAmount(maxDiscount)
                .validFrom(from)
                .validUntil(until)
                .status(VoucherStatus.ACTIVE)
                .requireCombo(requireCombo)
                .build());
    }

    private void assign(User user, Voucher voucher) {
        userVoucherRepository.save(UserVoucher.builder()
                .user(user)
                .voucher(voucher)
                .status(UserVoucherStatus.AVAILABLE)
                .claimedAt(LocalDateTime.now())
                .build());
    }
}
