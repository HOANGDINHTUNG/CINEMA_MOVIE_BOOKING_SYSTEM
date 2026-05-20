package com.re.cinemamoviebookingsystem.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cập nhật CHECK constraint bookings.status để cho phép HELD (đang thanh toán).
 * Hibernate ddl-auto không sửa được constraint có sẵn trên MySQL.
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class BookingStatusConstraintMigration implements ApplicationRunner {

    private static final String HELD = "HELD";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!constraintNeedsUpdate()) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE bookings DROP CHECK chk_booking_status");
        } catch (Exception ex) {
            log.debug("Drop chk_booking_status (may already be dropped): {}", ex.getMessage());
        }
        jdbcTemplate.execute("""
                ALTER TABLE bookings
                ADD CONSTRAINT chk_booking_status
                CHECK (status IN ('HELD', 'PENDING', 'PAID', 'CANCELLED'))
                """);
        log.info("Updated bookings.chk_booking_status to include HELD");
    }

    private boolean constraintNeedsUpdate() {
        try {
            String clause = jdbcTemplate.query(
                    """
                            SELECT cc.CHECK_CLAUSE
                            FROM information_schema.CHECK_CONSTRAINTS cc
                            JOIN information_schema.TABLE_CONSTRAINTS tc
                              ON cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                             AND cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
                            WHERE tc.TABLE_SCHEMA = DATABASE()
                              AND tc.TABLE_NAME = 'bookings'
                              AND tc.CONSTRAINT_NAME = 'chk_booking_status'
                            LIMIT 1
                            """,
                    rs -> {
                        if (!rs.next()) {
                            return null;
                        }
                        return rs.getString(1);
                    });
            if (clause == null) {
                return true;
            }
            return !clause.toUpperCase().contains(HELD);
        } catch (Exception ex) {
            log.warn("Could not inspect chk_booking_status, attempting migration: {}", ex.getMessage());
            return true;
        }
    }
}
