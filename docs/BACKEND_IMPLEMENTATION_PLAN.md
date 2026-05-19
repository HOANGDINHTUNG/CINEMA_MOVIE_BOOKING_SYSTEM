# Backend Implementation Plan — Smart Cinema

> Tiến độ triển khai. Cập nhật khi hoàn thành từng phase.

## Lựa chọn đã chốt

- Spring Boot 4 + Thymeleaf MVC + Spring Security (session)
- MySQL `smart_cinema_db` + script tại `src/main/resources/db/`
- Thanh toán: ONLINE (PAID ngay) + COUNTER (PENDING → Staff xác nhận)
- Extension: VNPay sandbox, Async email, Cron giải phóng ghế, Dashboard SQL

## Tiến độ

- [x] Phase 0: Gradle, Security, MySQL config, SQL schema/seed, exception handling
- [x] Phase 1: Entities + Repositories (custom queries)
- [x] Phase 2: Auth, RBAC, Profile (CORE-01..03)
- [x] Phase 3: Admin Movie CRUD (CORE-04)
- [x] Phase 4: Showtime + conflict + CORE-08
- [x] Phase 5: Booking transaction + lock seats (CORE-06)
- [x] Phase 6: Booking history JOIN (CORE-07)
- [x] Phase 7: Cancel 24h + unlock (CORE-09)
- [x] Phase 8: Staff lookup + confirm counter
- [x] Phase 9: Admin revenue report (SQL aggregation)
- [x] Phase 10: Thymeleaf templates wired
- [x] Phase E: VNPay sandbox, @Async email, @Scheduled lock cleanup, Dashboard charts data

## Chạy ứng dụng

1. Import `src/main/resources/db/schema.sql` và `seed.sql` vào MySQL 8+
2. Sửa `spring.datasource.password` trong `application.properties`
3. `./gradlew bootRun`
4. Đăng nhập: `admin_demo` / `staff_phuong` / `customer_minh` — mật khẩu `123456`

## URL chính

| Role | URL |
|------|-----|
| Customer | `/customer/home`, `/customer/bookings` |
| Staff | `/staff/lookup` |
| Admin | `/admin/dashboard`, `/admin/movies`, `/admin/showtimes`, `/admin/reports` |
