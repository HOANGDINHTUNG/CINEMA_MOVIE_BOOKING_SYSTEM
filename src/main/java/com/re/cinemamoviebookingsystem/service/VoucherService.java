package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.dto.response.CheckoutVoucherOptionDto;
import com.re.cinemamoviebookingsystem.dto.response.VoucherPreviewDto;
import com.re.cinemamoviebookingsystem.dto.response.VoucherWalletItemDto;
import com.re.cinemamoviebookingsystem.entity.Booking;
import com.re.cinemamoviebookingsystem.entity.UserVoucher;
import com.re.cinemamoviebookingsystem.entity.Voucher;
import com.re.cinemamoviebookingsystem.enums.DiscountType;
import com.re.cinemamoviebookingsystem.enums.UserVoucherStatus;
import com.re.cinemamoviebookingsystem.enums.VoucherStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.UserVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final UserVoucherRepository userVoucherRepository;

    @Transactional(readOnly = true)
    public List<VoucherWalletItemDto> getWallet(Long userId) {
        return userVoucherRepository.findWalletByUserId(userId).stream()
                .map(uv -> toWalletItem(uv, null, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CheckoutVoucherOptionDto> getEligibleCheckoutOptions(Long userId,
                                                                     BigDecimal subtotal,
                                                                     boolean hasCombo) {
        if (userId == null || subtotal == null) {
            return List.of();
        }
        List<CheckoutVoucherOptionDto> options = new ArrayList<>();
        for (UserVoucher uv : userVoucherRepository.findWalletByUserId(userId)) {
            Optional<String> ineligible = ineligibleReason(uv, subtotal, hasCombo);
            if (ineligible.isPresent()) {
                continue;
            }
            BigDecimal discount = calculateDiscount(uv.getVoucher(), subtotal, hasCombo);
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Voucher v = uv.getVoucher();
            options.add(CheckoutVoucherOptionDto.builder()
                    .userVoucherId(uv.getUserVoucherId())
                    .code(v.getCode())
                    .title(v.getTitle())
                    .discountLabel(buildDiscountLabel(v))
                    .conditionLabel(buildConditionLabel(v))
                    .label(buildCheckoutLabel(v, discount))
                    .estimatedDiscount(discount)
                    .build());
        }
        options.sort(Comparator.comparing(CheckoutVoucherOptionDto::getEstimatedDiscount).reversed());
        if (!options.isEmpty()) {
            CheckoutVoucherOptionDto best = options.get(0);
            options.set(0, CheckoutVoucherOptionDto.builder()
                    .userVoucherId(best.getUserVoucherId())
                    .code(best.getCode())
                    .title(best.getTitle())
                    .discountLabel(best.getDiscountLabel())
                    .conditionLabel(best.getConditionLabel())
                    .label(best.getLabel())
                    .estimatedDiscount(best.getEstimatedDiscount())
                    .recommended(true)
                    .build());
        }
        return options;
    }

    @Transactional(readOnly = true)
    public VoucherPreviewDto preview(Long userId,
                                     Long userVoucherId,
                                     BigDecimal subtotal,
                                     boolean hasCombo) {
        if (userVoucherId == null) {
            return VoucherPreviewDto.builder()
                    .subtotal(subtotal)
                    .discount(BigDecimal.ZERO)
                    .grandTotal(subtotal)
                    .build();
        }
        UserVoucher uv = resolveForCheckout(userVoucherId, userId, subtotal, hasCombo);
        BigDecimal discount = calculateDiscount(uv.getVoucher(), subtotal, hasCombo);
        return VoucherPreviewDto.builder()
                .subtotal(subtotal)
                .discount(discount)
                .grandTotal(subtotal.subtract(discount))
                .label(buildCheckoutLabel(uv.getVoucher(), discount))
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVoucher resolveForCheckout(Long userVoucherId,
                                          Long userId,
                                          BigDecimal subtotal,
                                          boolean hasCombo) {
        UserVoucher uv = userVoucherRepository.findByIdAndUserId(userVoucherId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_INVALID, "Voucher không tồn tại"));
        Optional<String> reason = ineligibleReason(uv, subtotal, hasCombo);
        if (reason.isPresent()) {
            throw new BusinessException(ErrorCode.VOUCHER_INVALID, reason.get());
        }
        BigDecimal discount = calculateDiscount(uv.getVoucher(), subtotal, hasCombo);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VOUCHER_INVALID, "Voucher không áp dụng được cho đơn này");
        }
        return uv;
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal, boolean hasCombo) {
        if (voucher == null || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (ineligibleReason(voucher, UserVoucherStatus.AVAILABLE, subtotal, hasCombo).isPresent()) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        } else {
            discount = voucher.getDiscountValue();
        }
        return discount.min(subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markUsed(UserVoucher userVoucher, Booking booking) {
        if (userVoucher == null) {
            return;
        }
        userVoucher.setStatus(UserVoucherStatus.USED);
        userVoucher.setUsedAt(LocalDateTime.now());
        userVoucher.setUsedBooking(booking);
        userVoucherRepository.save(userVoucher);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreIfUsed(Booking booking) {
        if (booking == null || booking.getUserVoucher() == null) {
            return;
        }
        UserVoucher uv = booking.getUserVoucher();
        if (uv.getStatus() != UserVoucherStatus.USED) {
            return;
        }
        uv.setStatus(UserVoucherStatus.AVAILABLE);
        uv.setUsedAt(null);
        uv.setUsedBooking(null);
        userVoucherRepository.save(uv);
    }

    private VoucherWalletItemDto toWalletItem(UserVoucher uv, BigDecimal subtotal, boolean hasCombo) {
        Voucher v = uv.getVoucher();
        UserVoucherStatus displayStatus = resolveDisplayStatus(uv);
        boolean usableNow = subtotal != null
                && ineligibleReason(uv, subtotal, hasCombo).isEmpty()
                && calculateDiscount(v, subtotal, hasCombo).compareTo(BigDecimal.ZERO) > 0;

        return VoucherWalletItemDto.builder()
                .userVoucherId(uv.getUserVoucherId())
                .code(v.getCode())
                .title(v.getTitle())
                .description(v.getDescription())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .minOrderAmount(v.getMinOrderAmount())
                .maxDiscountAmount(v.getMaxDiscountAmount())
                .requireCombo(v.isRequireCombo())
                .validFrom(v.getValidFrom())
                .validUntil(v.getValidUntil())
                .status(displayStatus)
                .discountLabel(buildDiscountLabel(v))
                .conditionLabel(buildConditionLabel(v))
                .usableNow(usableNow)
                .build();
    }

    private UserVoucherStatus resolveDisplayStatus(UserVoucher uv) {
        if (uv.getStatus() == UserVoucherStatus.USED) {
            return UserVoucherStatus.USED;
        }
        LocalDateTime now = LocalDateTime.now();
        Voucher v = uv.getVoucher();
        if (uv.getStatus() == UserVoucherStatus.EXPIRED
                || v.getStatus() != VoucherStatus.ACTIVE
                || v.getValidUntil().isBefore(now)
                || v.getValidFrom().isAfter(now)) {
            return UserVoucherStatus.EXPIRED;
        }
        return UserVoucherStatus.AVAILABLE;
    }

    private Optional<String> ineligibleReason(UserVoucher uv, BigDecimal subtotal, boolean hasCombo) {
        return ineligibleReason(uv.getVoucher(), uv.getStatus(), subtotal, hasCombo);
    }

    private Optional<String> ineligibleReason(Voucher v,
                                            UserVoucherStatus walletStatus,
                                            BigDecimal subtotal,
                                            boolean hasCombo) {
        LocalDateTime now = LocalDateTime.now();
        if (walletStatus != UserVoucherStatus.AVAILABLE) {
            return Optional.of("Voucher đã được sử dụng hoặc hết hạn");
        }
        if (v.getStatus() != VoucherStatus.ACTIVE) {
            return Optional.of("Voucher không còn hiệu lực");
        }
        if (v.getValidFrom().isAfter(now)) {
            return Optional.of("Voucher chưa đến thời gian áp dụng");
        }
        if (v.getValidUntil().isBefore(now)) {
            return Optional.of("Voucher đã hết hạn");
        }
        if (v.isRequireCombo() && !hasCombo) {
            return Optional.of("Voucher chỉ áp dụng khi có combo trong đơn");
        }
        BigDecimal min = v.getMinOrderAmount() != null ? v.getMinOrderAmount() : BigDecimal.ZERO;
        if (subtotal != null && subtotal.compareTo(min) < 0) {
            return Optional.of("Đơn tối thiểu " + formatVnd(min));
        }
        return Optional.empty();
    }

    private String buildDiscountLabel(Voucher v) {
        if (v.getDiscountType() == DiscountType.PERCENT) {
            String label = "Giảm " + v.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
            if (v.getMaxDiscountAmount() != null) {
                label += " (tối đa " + formatVnd(v.getMaxDiscountAmount()) + ")";
            }
            return label;
        }
        return "Giảm " + formatVnd(v.getDiscountValue());
    }

    private String buildConditionLabel(Voucher v) {
        List<String> parts = new ArrayList<>();
        BigDecimal min = v.getMinOrderAmount() != null ? v.getMinOrderAmount() : BigDecimal.ZERO;
        if (min.compareTo(BigDecimal.ZERO) > 0) {
            parts.add("Đơn từ " + formatVnd(min));
        }
        if (v.isRequireCombo()) {
            parts.add("Có combo");
        }
        return parts.isEmpty() ? "Không yêu cầu tối thiểu" : String.join("\n", parts);
    }

    private String buildCheckoutLabel(Voucher v, BigDecimal discount) {
        return v.getCode() + " — " + buildDiscountLabel(v) + " (−" + formatVnd(discount) + ")";
    }

    private static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0đ";
        }
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(amount) + "đ";
    }
}
