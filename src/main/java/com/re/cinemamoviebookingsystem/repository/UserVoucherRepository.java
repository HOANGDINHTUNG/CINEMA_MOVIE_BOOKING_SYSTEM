package com.re.cinemamoviebookingsystem.repository;

import com.re.cinemamoviebookingsystem.entity.UserVoucher;
import com.re.cinemamoviebookingsystem.enums.UserVoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    @Query("""
            SELECT uv FROM UserVoucher uv
            JOIN FETCH uv.voucher v
            WHERE uv.user.userId = :userId
            ORDER BY uv.status ASC, v.validUntil ASC, uv.claimedAt DESC
            """)
    List<UserVoucher> findWalletByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT uv FROM UserVoucher uv
            JOIN FETCH uv.voucher v
            WHERE uv.userVoucherId = :id AND uv.user.userId = :userId
            """)
    Optional<UserVoucher> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT uv FROM UserVoucher uv
            JOIN FETCH uv.voucher v
            WHERE uv.user.userId = :userId AND uv.status = :status
            ORDER BY v.validUntil ASC
            """)
    List<UserVoucher> findByUserIdAndStatus(@Param("userId") Long userId,
                                            @Param("status") UserVoucherStatus status);
}
