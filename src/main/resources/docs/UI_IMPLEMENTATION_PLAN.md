# Kế hoạch giao diện NCC (HTML/CSS → Thymeleaf)

Cập nhật: 2026-05-18

## Đã có mockup + CSS (`static/css/`)

| Mockup | CSS | Route đích |
|--------|-----|------------|
| index.html | homepage.css | `/customer/home` |
| movie-details.html | movie-details.css | `/customer/movies/{id}`, `/customer/showtimes/{id}/seats` |
| payment.html | payment.css | `/customer/showtimes/{id}/checkout` |
| payment-success.html | payment-success.css | `/customer/bookings/{id}` |
| calendar.html | calendar.css | `/customer/calendar` |
| promotion.html | news.css | `/customer/promotions` |
| news.html | news.css | `/customer/news` |
| new-details.html | news.css | `/customer/news/{id}` |
| festival.html | festival.css | `/customer/festival` |
| festival-details.html | festival-details.css | `/customer/festival/{id}` |
| ticket-price.html | ticket-price.css | `/customer/ticket-price` |
| login/register | auth.css | `/login`, `/register` |
| admin-movies.html | admin-movies.css | `/admin/movies` |
| schedule-admin.html | admin-movies.css | `/admin/showtimes` |
| dashboard.html | admin-movies.css | `/admin/dashboard` |
| users-admin.html | — | Chưa có backend quản lý user |

## Trạng thái từng màn

### Khách hàng (CUSTOMER)

| # | Màn | Backend | UI NCC | Ghi chú |
|---|-----|---------|--------|---------|
| 1 | Trang chủ | ✅ | 🟢 90% | carousel indicators |
| 2 | Chi tiết phim | ✅ | 🟢 85% | showtime-slot NCC |
| 3 | Chọn ghế | ✅ | 🟢 85% | |
| 4 | Thanh toán | ✅ | 🟢 85% | tạm tính vé |
| 5 | Chi tiết vé | ✅ | 🟢 85% | VNPay + badge |
| 6 | Lịch sử vé | ✅ | 🟢 80% | status badges |
| 7 | Lịch chiếu | ✅ | 🟢 80% | |
| 8 | Hồ sơ | ✅ | 🟢 80% | profile-card |
| 9 | Khuyến mãi | ✅ | 🟢 80% | JSON + news.css |
| 10 | Tin tức | ✅ | 🟢 80% | |
| 11 | Chi tiết tin | ✅ | 🟢 80% | article-detail CSS |
| 12 | Liên hoan phim | ✅ | 🟢 80% | |
| 13 | Chi tiết liên hoan | ✅ | 🟢 80% | |
| 14 | Bảng giá vé | ✅ | 🟢 85% | Static |
| 15 | Đăng nhập / Đăng ký | ✅ | 🟢 80% | |

### Admin (ADMIN)

| # | Màn | Backend | UI NCC | Ghi chú |
|---|-----|---------|--------|---------|
| 1 | Dashboard | ✅ | 🟢 85% | Tailwind + sidebar |
| 2 | Quản lý phim (list) | ✅ | 🟢 85% | |
| 3 | Thêm/sửa phim | ✅ | 🟢 85% | |
| 4 | Lịch chiếu (list) | ✅ | 🟢 85% | |
| 5 | Tạo suất chiếu | ✅ | 🟢 85% | |
| 6 | Báo cáo doanh thu | ✅ | 🟢 85% | |
| 7 | Hồ sơ admin | ✅ | 🟢 85% | |
| 8 | Quản lý user | ❌ | ⬜ 0% | Chưa có API |

### Staff (STAFF)

| # | Màn | Backend | UI NCC | Ghi chú |
|---|-----|---------|--------|---------|
| 1 | Dashboard | ✅ | 🟢 85% | |
| 2 | Tra cứu vé | ✅ | 🟢 85% | |
| 3 | Kết quả tra cứu | ✅ | 🟢 85% | |
| 4 | In vé | ✅ | 🟢 75% | Print-friendly |
| 5 | Hồ sơ | ✅ | 🟢 85% | |

## Thứ tự triển khai

- **Giai đoạn A** — ✅ Trang tĩnh khách + menu header
- **Giai đoạn B** — ✅ Polish trang khách (profile, bookings, errors, KM chi tiết)
- **Giai đoạn C** — ✅ Layout admin + phim + suất chiếu
- **Giai đoạn D** — ✅ Dashboard, báo cáo, hồ sơ admin
- **Giai đoạn E** — ✅ Staff UI

## Ghi chú kỹ thuật

- Fragment: `fragments/customer/*`, `fragments/admin/*`, `fragments/staff/*`
- CSS chung khách: `css/customer/base.css`
- Không dùng class Bootstrap `.container` cho layout NCC
- Admin/Staff: Tailwind CDN + `css/admin/admin-movies.css`
- Script sửa thẻ HTML lỗi: `scripts/fix-html-tags.py`

## UX tiện lợi (2026-05-18, cập nhật 2)

- **Tìm/lọc phim**: ô tìm + thể loại trên Trang chủ & Lịch chiếu (`MovieRepository.searchActive`)
- **Giới hạn ghế**: `cinema.max-seats-per-booking=8` (backend + JS chọn ghế)
- **Hướng dẫn lần đầu**: tooltip từng bước (localStorage), trang home / seats / checkout

## UX tiện lợi (2026-05-18)

- Thanh **3 bước** đặt vé: Chọn ghế → Thanh toán → Hoàn tất
- **Chọn ghế**: tóm tắt ghế + tạm tính realtime, thanh cố định dưới màn hình (mobile)
- **Thanh toán**: đổi ghế, gợi ý PT, tổng dự kiến + combo (JS), xác nhận trước khi gửi
- **Trang chủ**: menu truy cập nhanh (lịch, vé, KM, giá)
- **Vé của tôi**: bảng cuộn ngang mobile, empty state có nút CTA
