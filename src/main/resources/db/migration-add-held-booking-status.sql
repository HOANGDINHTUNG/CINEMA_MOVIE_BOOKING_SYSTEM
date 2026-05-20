-- Chạy một lần trên DB đã tạo trước khi có trạng thái HELD (Đang thanh toán).
-- MySQL 8.0.16+

ALTER TABLE bookings DROP CHECK chk_booking_status;

ALTER TABLE bookings
    ADD CONSTRAINT chk_booking_status
        CHECK (status IN ('HELD', 'PENDING', 'PAID', 'CANCELLED'));
